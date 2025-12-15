# CODING_GUIDE.md

Implementation conventions and testing rules for 2.x.

---

## 1) Repo layout (recommended)

- services/<service-name>/
- realtime/gateway/, realtime/shard/
- shared/ (minimal: tracing + error mapping only)
- contracts/
- scripts/, tools/

Hard rule:
- shared libraries must remain small to avoid a distributed monolith.

---

## 2) Service implementation rules (2.0+)

- Each service:
  - owns its schema and migrations
  - has its own DB credentials
  - exposes health/readiness endpoints
  - emits traces/metrics/logs with traceId correlation

- No cross-service DB queries. Use gRPC or events.

---

## 3) gRPC rules

- Protos are the source of truth (contracts/services/**).
- Trace propagation is mandatory (traceId/correlationId).
- Error mapping must be explicit and stable:
  - internal error -> gRPC status -> external error envelope (edge-api)

Timeout/retry policy:
- must be explicit in config (no hidden defaults)
- v2.3 will standardize and enforce policies via tests/drills

---

## 4) Event rules (v2.1+)

- Outbox write is transactional with domain write.
- Relay publish is retryable.
- Consumers must:
  - be idempotent by eventId
  - implement bounded retry
  - route poison events to DLQ
  - expose metrics: lag/retry/DLQ/duplicate drops

---

## 5) Database/migrations rules

- Migrations are per service:
  - services/<svc>/db/migration (or equivalent)
- A service may only migrate its own schema.
- Any data migration must include:
  - backfill job
  - verification checks
  - rollback notes

---

## 6) Release rules (2.x)

- Every cutover must have:
  - feature flag / routing toggle
  - rollback plan + runbook
  - smoke test green before rollout

---

## 7) Required scripts (remove ambiguity)

Repo MUST provide:
- scripts/test-all.sh
- scripts/smoke.sh
- scripts/contract-test.sh
- scripts/migration-test.sh

Additional per-version:
- v2.2+: tools/drills/ + runbooks/
- v2.6: tools/loadtest/ + capacity notes in runbooks/
