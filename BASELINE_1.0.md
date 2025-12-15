# BASELINE_1.0.md

Minimal stable facts about the already-implemented `v1.0.0`.
Purpose: reduce context size while preventing accidental contract breakage.

This file MUST NOT contain long histories. It should only point to canonical contracts and fast checks.

---

## 1) v1.0.0 meaning

- v1.0.0 is implemented and considered the baseline.
- Post-1.0 work must preserve v1.0 external contracts unless a later version explicitly changes them.

Canonical contract storage is defined by `CONTRACTS.md`.

---

## 2) Required contract files (must exist)

These files MUST exist and be correct before starting v1.1.0:

- `contracts/openapi.v1.0.0.yaml`
- `contracts/ws.v1.0.0.md`
- `contracts/db.v1.0.0.md`

If any is missing:
- create it by extracting facts from the current implementation,
- and add/adjust tests to ensure it stays correct.

---

## 3) Runtime topology (as implemented in v1.0.0)

Record only what matters for compatibility and migration:

- Backend: Spring Boot monolith (modular)
- Frontend: React + TypeScript SPA
- Infra (local/dev): Nginx + MariaDB + Redis + Docker Compose
- Realtime: WebSocket handled by the monolith
- Known limitation: realtime room/session ownership includes in-process memory assumptions

---

## 4) Known limitations (facts only)

- L1: realtime room/session ownership is not safely transferable between instances
- L2: async/event pipeline is not hardened to “ops grade” (retry/DLQ/idempotency/outbox)
- L3: observability is not yet SLO+alert+runbook+drill grade
- L4: security/audit is baseline (not operator-grade)
- L5: DR/multi-region not implemented

---

## 5) “Do not break” smoke checklist

Any post-1.0 change must keep these green:

- login works (success + failure)
- matchmaking starts a match
- realtime match can complete
- match result is persisted and visible
- WS behavior matches `contracts/ws.v1.0.0.md`
- DB migrations run clean on a fresh database
- tests pass

---

## 6) Standard commands (must exist)

To avoid ambiguity, the repo MUST provide these scripts (create if missing):

- `./scripts/test-backend.sh`
- `./scripts/test-frontend.sh`
- `./scripts/test-all.sh`
- `./scripts/smoke.sh` (minimal e2e flow description or automation)

This file should only reference these scripts, not reinvent command lists.
