# AGENTS.md

Rules for AI coding agents working in this repository.

---

## 1) Mandatory reading order

Agents MUST read, in order:

1. `00_INDEX.md`
2. `AGENTS.md`
3. `STACK_DESIGN.md`
4. `CONTRACTS.md`
5. `PRODUCT_SPEC.md`
6. `CODING_GUIDE.md`
7. `VERSIONING.md`
8. `BASELINE_1.0.md`
9. relevant docs under `design/**` and `runbooks/**`

---

## 2) Hard constraints

### 2.1 No stack drift
Follow `STACK_DESIGN.md`. Without explicit human instruction:
- do NOT change backend away from Spring Boot
- do NOT change primary DB away from MariaDB
- do NOT introduce Kafka before v2.1.0
- do NOT invent runtime components not defined in `VERSIONING.md`

### 2.2 Version scope
Work MUST target exactly one version in `VERSIONING.md`.
Do NOT mix multiple versions in one change set.

### 2.3 External contract safety
If you change any external contract (REST/WS/DB):
- update the relevant file under `contracts/` first
- then implement code + tests
- then update design docs

---

## 3) Non-negotiable: Testing gate

A change is incomplete unless tests are added and pass.

Minimum expectations:
- REST changes: controller/service tests + integration test (DB/Redis as needed)
- Realtime changes: deterministic simulation test + WS integration test
- Async pipeline changes: outbox/consumer idempotency + retry/DLQ tests
- Scale/ops versions: at least one load test + at least one drill scenario

Flaky tests are unacceptable:
- realtime core logic MUST NOT depend on sleeps/timeouts for correctness

---

## 4) Standard workflow (must follow)

1) Declare the target version.
2) Produce a short plan: code changes + tests + docs.
3) Implement code.
4) Implement tests.
5) Run tests and fix until green.
6) Write/update required design/runbook docs.
7) Report:
   - what changed
   - how to run
   - how to test
   - (if applicable) how to run load tests/drills

---

## 5) Context minimization rule

- Keep main docs concise.
- Move verbose/outdated text to `docs/archive/` (per 00_INDEX policy).
- Do not remove or archive `contracts/**` or DB migrations.
