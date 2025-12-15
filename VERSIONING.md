# VERSIONING.md

Minor versions only. Patch versions are bugfix/refactor/docs/tests only.

Assumption: v1.5 is complete (see BASELINE_1.5.md).

---

## Definition of Done (applies to every minor version)

DONE means:
- scope matches this file
- contracts/** updated if behavior/IO changes
- tests added/updated and pass
- required design/runbooks exist
- cutover includes rollback plan where applicable
- execution is reproducible with scripts/ and tools/

---

# 2.x — Fully specified

## v2.0 — Hybrid MSA foundation (stepwise decomposition)

Goal:
- introduce service boundaries with real data ownership
- keep external REST/WS stable by default

Locked decisions:
- internal sync: gRPC + Protobuf
- no cross-service DB queries
- realtime-gateway validates access tokens locally using identity keys
- shard persists match results via match-service gRPC

Scope (minimum deliverables):
1) edge-api acts as the single external REST boundary (and strangler host).
2) Extract these services with real ownership:
   - identity-service (identity_* schema)
   - match-service (match_* schema + MatchEnded outbox write)
   - chat-service (chat_* schema)
3) Per-service migrations + DB credentials.
4) edge-api calls extracted services via gRPC for the extracted domains.
5) Keep external behavior intact (smoke flow remains green).

Contracts (must be produced/updated by agent based on code changes):
- contracts/services/identity/v2.proto
- contracts/services/match/v2.proto
- contracts/services/chat/v2.proto
- contracts/db/db.v2.md updated for ownership summary
- external contracts updated only if behavior truly changes (avoid if possible)

Required tests (gate):
- scripts/contract-test.sh passes (proto compile + openapi validation)
- each extracted service boots with clean schema (integration test)
- edge-api -> gRPC -> service smoke test
- scripts/smoke.sh (login -> match -> history)
- traceId continuity proven by at least one integration test
- scripts/migration-test.sh if any data is moved/backfilled

Required docs:
- design/backend/v2.0-service-boundaries.md
- design/backend/v2.0-grpc-contracts.md
- runbooks/v2.0-cutover-and-rollback.md

---

## v2.1 — Event bus introduction (Kafka) + ops-grade consumers

Goal:
- introduce Kafka for event-driven scaling with strict idempotency and DLQ ops

Scope:
- Kafka introduced.
- outbox relay publishes to Kafka topics.
- consumers run in owning services (rating/replay/notifications/abuse, as implemented).
- DLQ + bounded retry + idempotency enforced.

Contracts (agent-produced from code changes):
- contracts/events/envelope.v1.md
- contracts/events/match-ended.v1.json (required)
- additional event schemas for any new consumers

Required tests (gate):
- Kafka integration tests (Testcontainers):
  - duplicate delivery -> idempotency holds
  - consumer restart -> no double-apply
  - retry -> DLQ routing
- broker outage drill documented (expected behavior)
- dashboards/alerts include lag and DLQ metrics (as applicable)

Required docs:
- design/backend/v2.1-kafka-topics-and-envelope.md
- runbooks/v2.1-dlq-and-lag-ops.md

---

## v2.2 — Multi-region & DR (active-passive first)

Goal:
- DR readiness proven by drills with measured RTO/RPO
- region-fixed realtime with control/data plane split

Scope:
- define control-plane vs data-plane split
- active-passive for at least one critical path:
  - identity-service + match-service persistence is the minimum recommended target
- routing strategy documented (DNS/traffic manager)
- DR drills executed and measured

Required drills (gate):
- DR runbook + measured results:
  - RTO (time to recover)
  - RPO (data loss window)
- failover + rollback procedure validated
- data integrity checklist included

Required docs:
- design/infra/v2.2-multi-region-architecture.md
- runbooks/v2.2-dr-drills.md

---

## v2.3 — Resilience hardening (prevent cascading failures)

Goal:
- prevent cascading failures in a multi-service system

Scope:
- standardize timeouts/retries for gRPC clients (explicit config)
- circuit breaker + bulkhead + load shedding policies
- backpressure rules for realtime gateway/shard paths
- fault injection drills:
  - downstream timeout
  - DB connection pool exhaustion
  - partial Kafka outage (if v2.1 exists)

Required tests/drills (gate):
- resilience integration tests:
  - retries bounded
  - circuit breaker opens
  - edge-api returns stable external error envelope
- at least 2 reproducible drills under tools/drills/ with runbooks

Required docs:
- design/sre/v2.3-resilience-standards.md
- runbooks/v2.3-chaos-drills.md

---

## v2.4 — Security & compliance hardening (production reality)

Goal:
- reach a large-service baseline for security and compliance

Scope:
- service-to-service auth (mTLS or signed service tokens) + authorization model
- secrets/key management and rotation
- PII policy (classification/masking/retention + encryption where applicable)
- supply-chain security baseline (SCA/image scan/SBOM policy + CI gate)

Required tests (gate):
- authn/z parity tests (service-to-service + external)
- audit log coverage remains complete for sensitive/admin actions
- CI security gates produce artifacts and fail by policy for critical issues

Required docs:
- design/security/v2.4-security-baseline.md
- runbooks/v2.4-key-rotation-and-incident.md

---

## v2.5 — Data platform & experimentation (growth features)

Goal:
- enable growth loops safely: analytics + feature flags + experiments

Scope:
- analytics event pipeline (schema-defined, validated)
- feature flags with rollout/rollback patterns
- experiment assignment + exposure logging
- privacy constraints for analytics (masking/sampling/retention)

Constraints (to keep implementation tractable):
- analytics storage is allowed in MariaDB (analytics schema) or persisted event files; no new heavy warehouse runtime.

Required tests (gate):
- analytics event schema validation tests (required fields)
- exposure logging correctness (no duplicate exposure; traceable)
- privacy constraints validation (masking/sampling)

Required docs:
- design/backend/v2.5-analytics-and-experiments.md
- runbooks/v2.5-feature-flag-ops.md

---

## v2.6 — Edge, scale & cost discipline (operate like a big service)

Goal:
- edge correctness + continuous capacity proof + cost observability signals

Scope:
- caching strategy:
  - cache keys, invalidation, safety rules
  - implemented via ingress/nginx rules (CDN-equivalent behavior documented)
- WAF/rate limit/bot defenses standardized (edge + app)
- continuous load testing baseline:
  - WS connect storm
  - steady-state message rate
  - API burst
- capacity model and SLO regression checks included
- cost observability signals (metrics-level) per service/topic/region

Required tests/drills (gate):
- load tests runnable under tools/loadtest/ with documented thresholds
- at least 1 edge failure drill documented (e.g., rate-limit misconfig, cache invalidation issue)
- capacity report stored in runbooks/

Required docs:
- design/infra/v2.6-edge-cache-waf.md
- runbooks/v2.6-loadtest-and-capacity.md

---

# 3.x — Summaries only (no detailed scope here)

## v3.0 — Cell architecture introduction
- introduce cells to limit blast radius (per-region/per-tenant partitioning)
- cell-level capacity planning and isolation
- begin partial active-active for control-plane reads (not full system)

## v3.1 — Active-active control plane (partial)
- identity/config reads become multi-region active-active with explicit consistency constraints
- write paths remain constrained to avoid global write conflicts

## v3.2 — Global data & experimentation maturity
- mature analytics pipelines, anomaly detection, automated experiment guardrails
- automated privacy/compliance enforcement across pipelines
