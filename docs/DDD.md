# Domain-Driven Design (DDD) Specification
## Project: TeleShield - Call Interception & Rule Evaluation Domain
**Document Version:** 2.0.0 (Platform & Framework Agnostic)  
**Status:** Approved / Specification Standard

---

## 1. Strategic Domain Design & Bounded Contexts

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          TeleShield Domain Boundary                         │
│                                                                             │
│  ┌─────────────────────────────────┐   ┌─────────────────────────────────┐  │
│  │   Call Screening Context        │   │   Rule Management Context       │  │
│  │   (Core Domain)                 │   │   (Supporting Context)          │  │
│  │                                 │   │                                 │  │
│  │   - CallerIdentifier (VO)       │   │   - ScreeningRule (Aggregate)   │  │
│  │   - ScreeningVerdict (VO)       │   │   - RuleType (Value Object)     │  │
│  │   - ScreeningEngine (Domain Svc)│   │   - PatternExpression (VO)      │  │
│  └────────────────┬────────────────┘   └────────────────┬────────────────┘  │
│                   │                                     │                   │
│                   │               Emits Domain Event    │                   │
│                   │               (CallBlockedEvent)    │                   │
│                   ▼                                     ▼                   │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                     Audit & Telemetry Context                         │  │
│  │                     (Generic Subdomain)                               │  │
│  │                                                                       │  │
│  │   - BlockedCallRecord (Aggregate Root)                                │  │
│  │   - RetentionPolicy (Domain Policy)                                   │  │
│  │   - ScreeningStatistics (Read Model / View)                           │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Ubiquitous Language

| Term | Definition |
|---|---|
| **Caller Identifier** | A representation of an incoming caller's identity (e.g., telephone number, SIP URI, or anonymous flag). |
| **Canonical Identifier** | The normalized string representation stripped of formatting, punctuation, and non-dialable characters. |
| **Screening Rule** | A user-defined directive containing a pattern expression, rule type, operational state, and usage metrics. |
| **Whitelist Rule** | A high-priority rule that explicitly guarantees exemption from all blocking policies. |
| **Wildcard Expression** | A declarative string using `*` (matching 0 or more characters) and `?` (matching exactly one character). |
| **Screening Verdict** | The definitive, immutable outcome of evaluating a caller against active rules (`ALLOW`, `BLOCK`). |
| **Blocked Call Record** | An immutable audit entry capturing the occurrence of an intercepted and blocked call. |

---

## 3. Tactical Domain Model

### 3.1 Aggregate Roots & Entities

#### Aggregate Root: `ScreeningRule`
- **Identity:** `RuleId` (Unique identifier / UUID / Integer ID)
- **Attributes:**
  - `pattern: PatternExpression` (Value Object)
  - `label: String` (Descriptive name / annotation)
  - `ruleType: RuleType` (`EXACT`, `PREFIX`, `WILDCARD`, `REGEX`, `UNKNOWN_PRIVATE`)
  - `isWhitelist: Boolean` (Determines if this rule allows or blocks matches)
  - `isEnabled: Boolean` (Active state toggle)
  - `timesTriggered: Integer` (Running counter of matching screening events)
  - `createdAt: Timestamp`
  - `lastTriggeredAt: Timestamp?`
- **Business Invariants:**
  - A rule of type `EXACT`, `PREFIX`, `WILDCARD`, or `REGEX` cannot have an empty pattern.
  - A `WILDCARD` or `REGEX` pattern must be syntactically valid and compile without errors.
  - Incrementing `timesTriggered` updates `lastTriggeredAt` to the event timestamp.

#### Aggregate Root: `BlockedCallRecord`
- **Identity:** `RecordId`
- **Attributes:**
  - `callerIdentifier: String` (Raw incoming caller value)
  - `timestamp: Timestamp` (Epoch time of screening)
  - `matchedRuleId: RuleId` (Reference to the triggering `ScreeningRule`)
  - `matchedPatternSnapshot: String` (Snapshot of the rule pattern at time of interception)
  - `matchedLabelSnapshot: String` (Snapshot of rule label)
- **Business Invariants:**
  - Records are write-once, read-many (immutable ledger).

---

### 3.2 Value Objects

#### Value Object: `CallerIdentifier`
- **Properties:**
  - `raw: String`
  - `canonical: String` (Computed via normalization: only numeric digits and leading `+`)
  - `isAnonymous: Boolean` (True if value indicates private, unknown, withheld, or empty caller)
- **Equality:** Pure value equality based on `canonical` and `isAnonymous`.

#### Value Object: `PatternExpression`
- **Properties:**
  - `expression: String`
- **Behavior:**
  - `matches(target: String, type: RuleType): Boolean`
  - Encapsulates exact comparison, prefix matching, wildcard translation (`*` -> `.*`, `?` -> `.`), and regex matching.

#### Value Object: `ScreeningVerdict`
- **Variants:**
  - `Allowed(reason: String)`
  - `Whitelisted(rule: ScreeningRule)`
  - `Blocked(matchedRule: ScreeningRule, executionDurationMs: Integer)`

---

### 3.3 Domain Services

#### `ScreeningEngine` (Core Domain Service)
- **Responsibility:** Executes rule evaluation pipeline against an incoming `CallerIdentifier`.
- **Logic:**
  1. If master screening is inactive -> Return `Allowed("Master screening disabled")`.
  2. Evaluate active Whitelist rules against canonical caller -> If matched, return `Whitelisted(rule)`.
  3. If caller `isAnonymous` and block-anonymous policy is active -> Return `Blocked(anonymousRule)`.
  4. Iterate active Block rules -> If any rule matches, return `Blocked(matchedRule)`.
  5. If no rules match -> Return `Allowed("No matching rules")`.

---

### 3.4 Domain Events

1. **`CallScreenedEvent`**:
   - Emitted when any call screening evaluation completes.
   - Payload: `callerIdentifier`, `verdict`, `timestamp`, `durationMs`.

2. **`CallBlockedEvent`**:
   - Emitted when a call is blocked.
   - Consumers handle side-effects:
     - Rule Management context increments `ScreeningRule.timesTriggered`.
     - Audit & Telemetry context appends a new `BlockedCallRecord`.

---

## 4. Repository Contracts (Domain Ports)

```
interface ScreeningRuleRepository {
    findActiveRules(): List<ScreeningRule>
    findWhitelistRules(): List<ScreeningRule>
    findById(id: RuleId): ScreeningRule?
    save(rule: ScreeningRule): RuleId
    delete(id: RuleId): Boolean
    incrementTriggerCount(id: RuleId, timestamp: Timestamp): Void
}

interface BlockedCallRecordRepository {
    getAllRecords(limit: Integer, offset: Integer): List<BlockedCallRecord>
    save(record: BlockedCallRecord): RecordId
    delete(id: RecordId): Boolean
    purgeOlderThan(cutoffTimestamp: Timestamp): Integer
}

interface SystemConfigurationRepository {
    isMasterScreeningEnabled(): Boolean
    isBlockUnknownEnabled(): Boolean
    getLogRetentionDays(): Integer
    saveConfiguration(config: Configuration): Void
}
```
