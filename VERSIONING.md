# VERSIONING.md

Defines the roadmap and the exact scope for each version.

---

## 1) Global rules

- Work targets exactly one version.
- PATCH versions: bugfix/refactor/docs/tests only (no new features).
- Definition of Done applies to every version.

---

## 2) Definition of Done (non-negotiable)

A version is DONE only if:
- scope matches this document
- contract files under `contracts/` are updated if needed
- tests are added/updated and pass
- required load tests/drills exist (when specified)
- required design/runbook docs exist

---

## 3) Roadmap (post v1.0.0, priority order)

- v1.1.0 Realtime Scale-out
- v1.2.0 Observability & SRE
- v1.3.0 Async & Data Robustness
- v1.4.0 Platformization
- v1.5.0 Security & Audit
- v2.0.0 Service Decomposition
- v2.1.0 Event Bus
- v2.2.0 Multi-region & DR

---

## 4) v1.1.0 – Realtime Scale-out (Gateway + Shard + State/Handoff)

### Goal
Horizontally scalable realtime with explicit failure semantics.

### Hard decisions (locked for clarity)
- gateway<->shard messaging: **Redis Streams**
- handoff policy: **Deterministic terminate** (no resume in v1.1)

### Scope
- Deployables:
  - realtime-gateway (stateless WS)
  - game-session-shard (authoritative loop)
  - api-service remains for REST/domain
- Redis responsibilities:
  - shard registry + heartbeat
  - session -> shard ownership mapping
  - termination context storage (reason, timestamps, anti-abuse markers)
  - Streams:
    - request stream(s) gateway->shard
    - response stream(s) shard->gateway
- Reconnect semantics must be explicit:
  - rejoin window (configurable)
  - what happens when shard is lost
  - match end reason codes

### Required contract updates
- `contracts/ws.v1.1.0.md` MUST be created/updated for the new architecture.

### Required tests (gate)
- deterministic simulation test for game loop transitions
- WS integration tests:
  - handshake auth failures
  - reconnect flow
- shard kill scenario:
  - kill one shard during a match -> match terminates deterministically (per contract)
- load test:
  - WS connect storm + steady-state message rate (basic)

### Required docs
- `design/realtime/v1.1.0-architecture.md`
- `design/realtime/v1.1.0-protocol.md`
- `runbooks/v1.1.0-reconnect-and-failure.md`

---

## 5) v1.2.0 – Observability & SRE

### Goal
Operate the system like a real service: SLOs, alerts, runbooks, drills, trace correlation.

### Scope
- OpenTelemetry tracing across api/gateway/shard/worker
- Core metrics and dashboards
- SLO definitions + alert rules + runbooks (with links)

### Required tests/drills (gate)
- at least one integration test that proves trace correlation end-to-end
- 3 reproducible drills with scripts:
  1) DB degraded/unavailable
  2) Redis degraded/unavailable
  3) gateway/shard kill during activity

---

## 6) v1.3.0 – Async & Data Robustness (Outbox/Consumers/DLQ)

### Goal
Event-driven post-match processing with ops-grade reliability.

### Scope
- Outbox in MariaDB (transactional)
- Relay publisher
- Consumers (minimum): ranking, stats, notifications, abuse signals
- bounded retry + DLQ + idempotency

### Required tests (gate)
- outbox transactionality test
- consumer idempotency test (duplicate delivery)
- retry -> DLQ routing test
- DLQ reprocess/discard procedure doc

---

## 7) v1.4.0 – Platformization (K8s + IaC + GitOps)

### Goal
Reproducible deployments and safe rollouts.

### Gate
- local kind/minikube deployment path documented and working
- rolling update scenario validated for gateway reconnect policy

---

## 8) v1.5.0 – Security & Audit

### Goal
Operator-grade audit and security posture.

### Gate
- audit log tests for sensitive/admin actions
- rate limit enforcement tests
- REST+WS auth parity tests

---

## 9) v2.x summaries

- v2.0.0: service boundaries + contract tests
- v2.1.0: Kafka/PubSub allowed; at-least-once + idempotency only
- v2.2.0: active-passive DR first; DR drills with measured RTO/RPO
