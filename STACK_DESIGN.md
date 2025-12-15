# STACK_DESIGN.md

Authoritative stack and architecture constraints for v2.x execution.

---

## 1) Baseline (v1.5 already complete)

- Backend: Spring Boot (Java/Kotlin)
- Frontend: React + TypeScript
- DB: MariaDB (primary)
- Cache/coordination: Redis
- Edge routing: Nginx / Kubernetes Ingress
- Observability: OpenTelemetry + metrics + logs correlation
- Realtime: realtime-gateway + game-session-shard (scale-out)

---

## 2) Locked decisions for 2.x (to remove ambiguity)

- External client contract remains REST + WS by default (backward compatible).
- Internal synchronous calls: gRPC + Protobuf.
- Event bus: Kafka (allowed from v2.1).
- Reliability stance: at-least-once + idempotency (no exactly-once claims).
- Cross-service DB queries are forbidden (v2.0+).

---

## 3) Runtime components (2.0+)

Required deployables (names are canonical):
- services/edge-api
- services/identity-service
- services/match-service
- services/chat-service
- services/matchmaking-service (may remain “legacy inside edge-api” until extracted; see MIGRATION_PLAYBOOK)
- services/rating-service (best extracted once v2.1 exists)
- services/replay-worker
- realtime/gateway
- realtime/shard

Required infra:
- MariaDB
- Redis
- Kafka (v2.1+)
- Observability stack (Prometheus/Grafana/OTel collector or equivalent)

---

## 4) Authentication and trust model (must be consistent)

### External (client -> edge-api / realtime-gateway)
- Identity-service is the issuer of access tokens.
- edge-api and realtime-gateway validate access tokens locally (no per-request introspection).
- Key distribution:
  - identity-service exposes a key set endpoint for verification keys (JWKS-like).
  - edge-api and realtime-gateway cache verification keys with rotation support.
- WS handshake:
  - client provides access token; gateway validates and establishes session.
- Token expiry:
  - when access token expires, client refreshes via edge-api and reconnects WS if needed.

This is the default 2.x model. Any deviation must be explicitly defined in contracts + VERSIONING scope.

### Internal (service -> service)
- v2.0–v2.3: mTLS is recommended but optional.
- v2.4: service-to-service auth becomes mandatory (mTLS or signed service tokens), with documented rotation.

---

## 5) Data ownership (hard rule)

- One MariaDB cluster is allowed, but each service must have:
  - its own schema/database
  - its own DB credentials
  - its own migrations directory
- No service may query another service’s tables.

---

## 6) Multi-region / DR (v2.2)

- Data plane (regional): realtime-gateway + shard.
- Control plane (global-ish first): identity, admin/config, non-latency critical services.
- First target is active-passive with drills and measured RTO/RPO.
- Active-active is out of scope until v3.x.

---

## 7) Data platform & edge/cost (v2.5–v2.6) without “new heavy runtime”
To keep implementation tractable:
- v2.5 analytics storage is allowed in MariaDB (analytics schema) or as event files; vendor-specific warehouses are out of scope.
- v2.6 edge features are implemented via:
  - Nginx/Ingress caching rules (and documented CDN equivalence),
  - WAF/rate-limit policies at ingress + app,
  - continuous load tests and capacity model based on metrics.

---

## 8) Prohibited changes unless explicitly approved

- switching DB away from MariaDB
- replacing Spring Boot
- Kafka before v2.1
- cross-service DB queries
- active-active multi-region before v3.x
