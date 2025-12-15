# BASELINE_1.5.md

Minimal stable facts about the already-implemented v1.5 baseline.
Purpose: keep 2.x changes honest and prevent accidental contract breakage.

---

## v1.5 guarantees (must remain true unless explicitly changed)

- External client contracts (REST + WS) are stable.
- Realtime scale-out exists (gateway + shard) with explicit failure semantics.
- Observability/SRE baseline exists (traces/metrics/log correlation + drills).
- Async robustness exists (outbox + consumers + retry/DLQ + idempotency).
- Platformization exists (K8s + IaC + GitOps).
- Security & audit exists (audit logs, rate limits, OAuth consent/scope if applicable).

---

## Canonical external contracts (must exist before 2.0)

- contracts/external/openapi.v1.5.yaml
- contracts/external/ws.v1.5.md
- contracts/external/errors.md
- contracts/db/db.v1.5.md

If any is missing, agents must extract facts from code and commit them (with tests where applicable).

---

## Do-not-break smoke

Any 2.x change must keep these green:
- login works (success + failure)
- matchmaking starts a match
- realtime match completes deterministically (per WS contract)
- match result is persisted and visible
- DB migrations run clean on a fresh DB
- tests pass

---

## Required scripts (must exist)

- scripts/test-all.sh
- scripts/smoke.sh
- scripts/contract-test.sh
- scripts/migration-test.sh
