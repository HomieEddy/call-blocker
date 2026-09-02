# Architecture Requirements Document (ARD)
## Project: TeleShield - Call Screening & Interception Architecture
**Document Version:** 2.0.0 (Platform & Framework Agnostic)  
**Status:** Approved / Specification Standard

---

## 1. Architectural Style & Principles

TeleShield follows **Clean Architecture** (Hexagonal / Ports and Adapters) paired with **Unidirectional Data Flow (UDF)** principles. The architecture strictly decouples the core domain and evaluation engine from all platform-specific telephony APIs, UI presentation libraries, and storage backends.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Presentation / UI Layer                          │
│        (Declarative Views, State ViewModels, UI State Handlers)             │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Reads StateFlow / Reactive Streams
┌──────────────────────────────────────▼──────────────────────────────────────┐
│                            Application Use Cases                            │
│  - ScreenIncomingCallUseCase          - ManageBlockRulesUseCase             │
│  - ManageWhitelistUseCase             - SimulateCallUseCase                 │
│  - QueryBlockedLogsUseCase            - PurgeAuditLogsUseCase               │
└──────────────────┬───────────────────────────────────────────┬──────────────┘
                   │ Invokes                                   │ Reads/Writes
┌──────────────────▼───────────────────┐    ┌──────────────────▼──────────────┐
│             Domain Core              │    │          Domain Ports           │
│  - Pattern Matching Engine           │    │  - RuleRepositoryPort           │
│  - Whitelist & Blacklist Evaluator   │    │  - CallLogRepositoryPort        │
│  - Normalization Service             │    │  - SettingsRepositoryPort       │
└──────────────────────────────────────┘    └──────────────────▲──────────────┘
                                                               │ Implements
┌──────────────────────────────────────────────────────────────┴──────────────┐
│                    Infrastructure & Platform Adapters                       │
│  ┌─────────────────────────┐  ┌────────────────────┐  ┌──────────────────┐  │
│  │   OS Telephony Adapter  │  │ Local Persistence  │  │ Local Key-Value  │  │
│  │ (Platform Screening Hook)│ │ (SQL / Relational) │  │  (Settings Store)│  │
│  └─────────────────────────┘  └────────────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Layer Definitions & Boundaries

### 2.1 Domain Layer (Core & Innermost)
- **Role:** Pure business logic, entity models, and matching algorithms with zero external library dependencies.
- **Components:**
  - `RuleEvaluator`: Evaluates normalized caller IDs against sorted rule specifications.
  - `PatternParser`: Compiles wildcard/regex strings into executable pattern matchers.
  - `IdentifierNormalizer`: Canonicalizes phone identifiers (e.g., stripping non-numeric formatting characters).

### 2.2 Application Layer (Use Cases & Orchestration)
- **Role:** Orchestrates workflows between domain algorithms and infrastructure ports.
- **Key Use Cases:**
  - `ScreenIncomingCall(callerId, isPrivate)`: Core screening workflow with whitelist check, blacklist scan, counter increment, and log dispatch.
  - `AddRule(pattern, type, label, isWhitelist)`: Validates and saves a screening rule.
  - `SimulateCall(testCallerId)`: Non-persistent evaluation returning detailed matching breakdown.
  - `PurgeExpiredLogs(retentionDays)`: Removes audit logs older than the threshold.

### 2.3 Ports (Interfaces)
- **`RuleRepositoryPort`**: CRUD interface for persisted block/whitelist rules and usage counters.
- **`CallLogRepositoryPort`**: Append-only ledger interface for blocked calls with chronological query and pruning capabilities.
- **`SettingsRepositoryPort`**: Read/write interface for application-wide preferences (Master switch, unknown caller policy, retention period).
- **`TelephonyInterceptionPort`**: Outbound port defining telephony actions (e.g., accept, reject, suppress notification, suppress system call log).

### 2.4 Infrastructure Layer (Adapters)
- **Telephony Adapter:** Connects host platform telephony events (e.g., OS screening callbacks) to `ScreenIncomingCallUseCase`.
- **Database Adapter:** Implements repository ports using standard local relational or document storage (e.g., SQLite, Embedded Key-Value, JSON store).
- **Settings Adapter:** Implements settings port using local configuration storage.

### 2.5 Presentation Layer
- **Role:** Provides user-facing UI for managing rules, reviewing audit logs, simulating calls, and adjusting settings.
- **Pattern:** Model-View-ViewModel (MVVM) or Model-View-Intent (MVI) with unidirectional state propagation.

---

## 3. Screening Engine Pipeline & Lifecycle

```
[ Incoming Call Trigger ]
          │
          ▼
[ Normalize Caller ID ] ──────────────► Clean canonical string
          │
          ▼
[ Check Master Switch ] ──(Disabled)──► [ Decision: ALLOW ]
          │ (Enabled)
          ▼
[ Check Whitelist Rules ] ─(Matched)──► [ Decision: ALLOW (Whitelisted) ]
          │ (No Match)
          ▼
[ Is Caller Private/Hidden? ]
          ├─► (Yes & Block Unknown = True) ──► [ Decision: BLOCK (Private Rule) ]
          │
          ▼ (No or Block Unknown = False)
[ Check Active Block Rules ]
  ├─ 1. Exact Match Rules
  ├─ 2. Prefix Match Rules
  ├─ 3. Wildcard Mask Rules (*, ?)
  └─ 4. Regex Pattern Rules
          │
          ├─► (Any Match) ──► [ Decision: BLOCK ] ──► Async: Increment Counter & Append Log
          │
          └─► (No Match)  ──► [ Decision: ALLOW ]
```

---

## 4. Cross-Cutting Concerns & Non-Functional Design

### 4.1 Concurrency & Execution Contexts
- **Synchronous Screening Path:** The screening pipeline must execute on a prioritized background worker thread to meet the < 15ms latency deadline.
- **Asynchronous Audit Path:** Log insertion and counter increments must occur asynchronously without blocking the immediate screening response.

### 4.2 Error Handling & Fail-Open Strategy
- If a rule pattern is malformed or throws an execution exception during live evaluation, the engine catches the exception, logs an internal warning, and fails open (`ALLOW`), ensuring legitimate calls are never dropped due to software faults.

### 4.3 Data Portability
- Rule sets and audit logs must be exportable and importable via standard structured formats (e.g., JSON / CSV) to enable backup and platform migration.
