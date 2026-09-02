# Product Requirements Document (PRD)
## Project: TeleShield - Advanced Wildcard Call Screening & Prevention Engine
**Document Version:** 2.0.0 (Platform & Framework Agnostic)  
**Status:** Approved / Specification Standard

---

## 1. Executive Summary & Problem Statement

### 1.1 Problem Statement
Telephone users worldwide face an increasing deluge of unsolicited spam, automated robocalls, fraud campaigns, and neighbor-spoofing attacks:
1. **Exchange & Neighbor Spoofing:** Attackers cycle phone numbers using identical regional and exchange prefixes, rendering static exact-number blacklists ineffective.
2. **International Sweep & Toll Scams:** High-frequency spam originating from entire country or area code blocks.
3. **Data Privacy Risks:** Conventional caller-identification tools upload user address books and private call history to external cloud infrastructures.

### 1.2 Product Vision
**TeleShield** is an autonomous, privacy-first, zero-network-dependency call screening system. It enables users to define granular call interception rules using wildcards (`*`, `?`), regular expressions, prefix matching, exact numbers, and private/anonymous filters, supported by an immediate whitelist bypass mechanism and an interactive testing sandbox.

---

## 2. Target Audience & Personas

- **The Privacy-Focused Consumer:** Requires 100% local, on-device evaluation without transmitting metadata or contacts across the network.
- **The Targeted Mobile Subscriber:** Needs declarative pattern blocking (e.g., blocking an entire exchange while allowing known contacts) to defeat spoofing.
- **The High-Call-Volume Professional:** Manages complex incoming communication rules, requiring fast rule creation, whitelist overrides, and audit logs.

---

## 3. Core Capabilities & Value Propositions

1. **Zero External Data Egress:** All matching logic, audit records, and preference stores operate completely offline on the host device.
2. **Deterministic Pattern Engine:** High-performance wildcard and prefix evaluation capable of filtering millions of possible permutations with a single expression.
3. **Precedence-Driven Whitelist Exception:** Whitelisted identities strictly take priority over all active block rules.
4. **Zero-Side-Effect Simulator:** An interactive verification sandbox to test numbers against the current rule catalog before processing live calls.

---

## 4. Functional Requirements

### 4.1 Screening Engine & Pattern Hierarchy
- **FR-01 (Rule Types):** The engine must support five fundamental rule classes:
  - `EXACT`: Full numeric identity matching after normalization.
  - `PREFIX`: Matches any caller starting with a designated digit sequence (e.g., country or regional trunk).
  - `WILDCARD`: Multi-character (`*` matching 0 or more digits) and single-character (`?` matching exactly one digit) wildcard masks.
  - `REGEX`: Standard regular expression syntax for custom structural matches.
  - `UNKNOWN_PRIVATE`: Intercepts anonymous, suppressed, or unidentifiable caller signals.
- **FR-02 (Whitelist Overrides):** Whitelist rules take unconditional priority. Any incoming identifier matching an enabled Whitelist rule must be passed through immediately (`ALLOWED`).
- **FR-03 (Identifier Normalization):** All incoming caller values and user rule inputs must be normalized (removing visual separators such as spaces, hyphens, and parentheses, and standardizing international dial prefixes).
- **FR-04 (Rule Activation & Metrics):** Each rule must support dynamic enabling/disabling and maintain usage counters (`timesBlocked`, `lastTriggeredTimestamp`).

### 4.5 Telephony Interception Interface
- **FR-05 (Screening Decision Pipeline):** When an incoming call signal is intercepted:
  1. Determine if Master Screening is enabled. If disabled, return `ALLOW`.
  2. Check Whitelist records. If matched, return `ALLOW`.
  3. If caller identity is suppressed/empty, check Private/Unknown policies.
  4. Evaluate active block rules sequentially (Exact -> Prefix -> Wildcard -> Regex).
  5. Return decision: `ALLOW` or `BLOCK` (with options to reject call, suppress notifications, and omit system call log entry).
- **FR-06 (Audit Logging):** Every blocked call event must create an immutable log record capturing caller ID, timestamp, matched rule identifier, and descriptive pattern label.

### 4.3 Log Lifecycle & History Management
- **FR-07 (Audit Ledger View):** Provide a chronological audit log with caller ID, timestamp, and matched rule details.
- **FR-08 (Quick Actions & Promotion):** Provide one-tap promotion of unknown numbers in logs to either the Blocklist or Whitelist.
- **FR-09 (Automated Retention & Pruning):** Automatically purge log records older than a user-configured retention policy (e.g., 7, 14, 30, 90 days, or never).
- **FR-10 (Clipboard & Context Operations):** Allow users to copy numbers, modify associated rules, and delete individual log items.

### 4.4 In-App Call Simulator
- **FR-11 (Virtual Call Evaluation):** Enable users to input arbitrary numbers or simulate private calls to observe engine decisions in real time.
- **FR-12 (Evaluation Breakdown):** Output the exact verdict (`ALLOWED` / `BLOCKED`), the triggering rule metadata, and execution duration.

### 4.5 Configuration & Settings
- **FR-13 (Master Switch):** Global kill-switch to pause screening without deleting rules.
- **FR-14 (Action Preferences):** User-selectable behaviors on block (e.g., reject audio ring, hide notification, suppress OS call log).

---

## 5. Non-Functional Requirements

### 5.1 Latency & Performance
- **NFR-01 (Screening Deadline):** The screening engine must produce an `ALLOW` or `BLOCK` verdict within **< 15 milliseconds** of receiving an incoming call trigger.
- **NFR-02 (Memory Footprint):** Minimal background memory consumption during idle state.

### 5.2 Security & Data Privacy
- **NFR-03 (Zero Network Dependency):** The core screening service must function 100% offline without requiring internet access or third-party telemetry.
- **NFR-04 (Storage Isolation):** User rules and logs must reside exclusively in isolated, secure local storage.

### 5.3 Reliability
- **NFR-05 (Fault Isolation):** Parsing or execution failures on any single rule must fail open safely (`ALLOW`) without disrupting device telephony.

---

## 6. Verification Criteria
- **Rule Verification:** All wildcard expressions correctly resolve single-digit (`?`) and multi-digit (`*`) patterns.
- **Precedence Verification:** Whitelist rules consistently bypass any matching block rule.
- **Audit Verification:** Every blocked call increments the rule counter and creates a corresponding immutable log entry.
