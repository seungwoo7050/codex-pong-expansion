# MIGRATION_PLAYBOOK.md

How to migrate from v1.5 to 2.x safely (stepwise hybrid MSA).

This playbook is the operational truth for cutovers and rollbacks.

---

## Strategy: Strangler + stable external contract

- Keep external REST/WS stable.
- Convert the existing monolith into edge-api (or introduce edge-api in front).
- Extract one service at a time with:
  - contracts updated
  - tests added
  - feature-flag cutover
  - rollback plan + runbook

---

## Safety rules (non-negotiable)

- No cross-service DB queries.
- Every extraction must include:
  - gRPC contract (proto) and contract tests
  - per-service schema and migrations
  - integration tests and e2e smoke
  - rollback toggle and cutover runbook
- External contracts only change if explicitly scoped and tested.

---

## Recommended extraction order (2.x)

v2.0 (minimum):
1) identity-service (issuer + identity_*)
2) match-service (authoritative match_* + MatchEnded outbox)
3) chat-service (independent extraction, low coupling)

v2.1:
4) rating-service (event-driven from MatchEnded)
5) replay-worker improvements (event-driven + DLQ operations)

Later:
6) matchmaking-service extraction (higher coupling with realtime)

---

## Cutover pattern (per extracted service)

1) Bootstrap
- create service code + migrations + DB credentials
- define proto under contracts/services/**

2) Shadow mode (optional but preferred)
- edge-api writes/reads old path but also calls new service in “shadow”
- compare outputs (logs/metrics)

3) Feature flag cutover
- enable new service path for small cohort
- expand gradually

4) Freeze old tables (if migrating data)
- old tables become read-only and are deprecated

5) Rollback plan
- a single toggle routes traffic back to previous implementation
- rollback must be documented and testable

---

## Data migration pattern (when required)

1) Create new schema + migrations.
2) Backfill job (batch).
3) Shadow read + compare.
4) Cutover via feature flag.
5) Validate data integrity.
6) Keep rollback window until confidence is high.

---

## Required validations for any cutover

- scripts/contract-test.sh
- scripts/test-all.sh
- scripts/migration-test.sh (if data moved)
- scripts/smoke.sh (login -> match -> history)
- traceId continuity verification (edge -> service -> DB/outbox)
