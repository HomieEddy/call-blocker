# AGENTS.md

Guidelines for humans and AI agents working in this repository.

## Project

**TeleShield** — a privacy-first, zero-network-dependency call screening engine for Android.

- **Stack:** Kotlin + Jetpack Compose (Android). The domain engine is a pure-JVM module with zero Android dependencies.
- **Architecture:** Clean Architecture (Hexagonal) per `docs/ARD.md` and `docs/DDD.md`. Domain core -> application use cases -> ports (interfaces) -> infrastructure adapters (later milestones).
- **Primary docs:** `docs/PRD.md` (requirements), `docs/ARD.md` (architecture), `docs/DDD.md` (domain model). These are the source of truth for behavior.

### Domain language

Use the ubiquitous-language terms from `docs/DDD.md`: `CallerIdentifier`, `Canonical Identifier`, `ScreeningRule`, `Whitelist Rule`, `PatternExpression`, `ScreeningVerdict`, `BlockedCallRecord`. Test names and class names must use this vocabulary.

---

## Coding practices

- **Idiomatic Kotlin.** Follow the official Kotlin coding conventions. Prefer `val`, data classes, sealed classes/interfaces for verdicts and events, and expression bodies.
- **Type everything.** Explicit return types on public functions. No platform types leaking into the domain layer.
- **Layer boundaries are hard.** The `domain` package has no Android, framework, or persistence imports. Dependencies point inward: `application` -> `domain`, `ports` -> `domain`, adapters -> `ports`. Never reverse.
- **Constructor injection only.** No DI framework in the engine module (Hilt is introduced later, in the Android shell). Pass collaborators through constructors.
- **Fail open.** Any exception during rule evaluation must resolve to `ALLOW`, never drop a legitimate call. This is an NFR, not a nicety.
- **Pure functions in the engine.** Normalization and pattern matching are deterministic and side-effect free. Side effects (counter increments, log appends) happen in use cases / domain-event consumers, not in `ScreeningEngine`.
- **No comments unless they explain *why*.** Code should be self-documenting; comments only mark non-obvious intent or deliberate simplifications (`ponytail:` notes).
- **No magic strings.** Rule types, verdicts, and policies are enums/sealed types, not raw strings.

---

## TDD (mandatory)

We follow test-driven development: **red -> green -> refactor**, in vertical slices. No feature code without a failing test first.

### The loop

1. Write the failing test at the agreed seam.
2. Run it; confirm it fails for the right reason.
3. Write the minimum code to make it pass.
4. Refactor with the test green.
5. Commit only when green.

### Seams (test only at these public boundaries)

- `IdentifierNormalizer` — canonicalization, `+` handling, anonymous detection.
- `PatternExpression` — exact / prefix / wildcard / regex per `RuleType`.
- `ScreeningEngine` — whitelist precedence, master switch, private policy, rule ordering, fail-open.
- Application use cases — orchestration and domain-event dispatch.

Do not test private methods or internal collaborators. A test must survive refactoring without changing.

### Test rules

- **Framework:** JUnit 5 + `kotlin.test`. Use `@ParameterizedTest` for table-driven pattern cases.
- **Expected values must be independent** — literal known-good values, never recomputed the same way the code computes.
- **One slice at a time** — one seam, one test, one minimal implementation.
- **Command:** `./gradlew :engine:test`
- The `<15ms` screening deadline is verified by the engine benchmark, not a flaky unit assertion.

---

## Agent skills

Relevant installed skills and when to invoke them:

| Skill | Use when |
|---|---|
| `brainstorming` | Before any creative/feature work — clarify intent and design before code. |
| `tdd` / `test-driven-development` | Before writing any feature or bugfix. Governs the red-green loop above. |
| `writing-plans` | Multi-step implementation work after a design is approved. |
| `systematic-debugging` | Any bug, test failure, or unexpected behavior — before proposing a fix. |
| `frontend-design` / `mobile-app-ui-design` | Compose UI work (later milestones). |
| `requesting-code-review` / `receiving-code-review` | Verifying completed work / reviewing feedback. |
| `caveman` | Compressed communication mode — invoke for terse, token-efficient prose. |
| `caveman-commit` | Generating Conventional Commits messages. Auto-trigger on staging. |
| `caveman-review` | Compressed code review comments — one line per finding (location, problem, fix). |
| `using-superpowers` | Start of any session — establishes how to find and load skills before responding. |
| `verification-before-completion` | Before claiming work is done — run the build/tests and confirm output first. |
| `executing-plans` | Executing an approved `writing-plans` plan with review checkpoints. |
| `subagent-driven-development` | Executing plan tasks in parallel when they're independent. |
| `finishing-a-development-branch` | Merge / PR / cleanup decisions when a feature branch is complete. |
| `using-git-worktrees` | Isolating feature work from the workspace (later, once branches matter). |

Load the skill with the `skill` tool before doing the matching work. Skills inject their workflow; do not skip the load.

**Not installed / not applicable:** `mattpocock/skills` is a TypeScript/Total-TypeScript collection that isn't present in this environment and targets a different language. Do not reference it; use the Kotlin equivalents above instead.

---

## Version control

- **Commits:** Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`). Subject <= 50 chars, imperative mood. Body only when the *why* isn't obvious. Generate with `caveman-commit`.
- **Atomic commits.** Each commit is one vertical slice — test + implementation together, green. Never commit a red build.
- **Never commit:** secrets, API keys, build artifacts, IDE files (`.idea/`, `.gradle/`, `build/`).
- **Only commit when asked.** Never `git add`/`commit`/`push` proactively.
- Before committing, review `git status` and `git diff`; stage only intended files.
- **Branch:** `main` for stable. Feature work on short-lived branches when the Android shell lands; for the engine-first phase, committing to `main` is fine.

---

## Build & verification

- Run tests: `./gradlew :engine:test`
- Full build: `./gradlew build`
- After any change to the engine, run the test command before claiming completion. Never claim work is done on intent — run the command and report the output.
