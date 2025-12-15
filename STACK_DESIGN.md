# STACK_DESIGN.md

Authoritative tech stack and architecture constraints.

---

## 1) Baseline stack (v1.0.0)

Frontend:
- React + TypeScript SPA

Backend:
- Spring Boot (keep existing language choice; do not switch midstream)

Data:
- MariaDB (primary)
- Redis (cache/coordination)

Infra (local/dev):
- Nginx + Docker + Docker Compose

---

## 2) Allowed evolution (post v1.0.0)

### v1.1.0 Realtime Scale-out (target topology)
Runtime components:
- `api-service` (REST + core domain)
- `realtime-gateway` (WebSocket, stateless)
- `game-session-shard` (authoritative loop, horizontally scalable)

Routing:
- `/api/*` -> `api-service`
- `/ws/*`  -> `realtime-gateway`

State/routing substrate:
- Redis is used for:
  - shard registry (heartbeat)
  - session -> shard ownership mapping
  - session snapshot/termination context storage
  - gateway<->shard messaging substrate

#### v1.1.0 hard choices (to remove ambiguity)
- gateway<->shard messaging: **Redis Streams**
- failure/handoff policy: **Deterministic terminate** (no resume in v1.1)

Reason: simplest to implement, test, and operate; prevents “half-resume” correctness bugs.

Local dev may use single Redis instance; production may use Redis Cluster.

### v1.2.0 Observability/SRE
- OpenTelemetry tracing
- Prometheus + Grafana
- Correlation: traceId across HTTP/WS/async paths

### v1.3.0 Async & data robustness
- Outbox pattern (MariaDB)
- Relay publisher + consumer workers
- DLQ/retry/idempotency enforced

### v1.4.0 Platformization
- Kubernetes (kind/minikube local + managed cluster compatible)
- Terraform + GitOps (ArgoCD or equivalent)
- rollout safety (probes + graceful shutdown)

### v1.5.0 Security & Audit
- audit logs (append-only policy)
- rate limiting policies (login/chat/ws)
- OAuth consent/scope (if OAuth exists)

### v2.x
- hybrid MSA boundaries + contract tests
- Kafka/PubSub only from v2.1.0 onward
- DR/multi-region only from v2.2.0 onward

---

## 3) Prohibited changes (unless explicitly approved)

- switching DB away from MariaDB
- replacing Spring Boot backend
- introducing Kafka before v2.1.0
- inventing new runtime components not defined in VERSIONING.md
