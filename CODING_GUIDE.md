# CODING_GUIDE.md

Implementation conventions and testing rules.

---

## 1) Core rules

- Keep architecture layered (controller -> application/service -> domain -> infra).
- No hidden global state for realtime after v1.1.
- External contracts are sacred: REST/WS/DB changes must be reflected in `contracts/`.

---

## 2) Realtime rules (v1.1+)

### 2.1 Deterministic tick loop
- Core loop must be testable without sleeps.
- Use injected time source or step-based simulation.

### 2.2 Standard message envelope (mandatory)
All gateway<->shard messages must include:
- messageId (uuid)
- type
- occurredAt
- sessionId
- traceId (or correlationId)
- payload

### 2.3 Delivery stance
- At-least-once can happen; consumers must be idempotent where relevant.
- Ordering assumptions must be explicit and tested.

---

## 3) Async pipeline rules (v1.3+)

- Outbox record write must be in the same DB transaction as the domain write.
- Relay publishing must be retryable.
- Consumers must implement:
  - idempotency (eventId)
  - bounded retry
  - DLQ for poison events
  - metrics/logs for retry/DLQ counts

---

## 4) Standard scripts (remove “how do I run tests?” ambiguity)

Repo MUST provide:
- `./scripts/test-backend.sh`
- `./scripts/test-frontend.sh`
- `./scripts/test-all.sh`
- `./scripts/smoke.sh`

Scale/ops versions MUST additionally provide:
- `./scripts/loadtest-ws.sh` (or `tools/loadtest/` runner)
- `./scripts/drill-*.sh` under `tools/drills/`

---

## 5) Testing rules (hard gate)

- REST changes:
  - controller tests + service tests + integration test (DB/Redis as needed)
- Realtime changes:
  - deterministic simulation tests
  - WS integration tests (auth + reconnect)
- Async pipeline changes:
  - outbox transactional test
  - consumer idempotency test
  - retry->DLQ test

Flaky tests are unacceptable.
If a test needs sleeps to pass, redesign the code/test harness.
