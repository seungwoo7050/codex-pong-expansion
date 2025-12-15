# PRODUCT_SPEC.md

Defines what this service is and the product-level requirements.

---

## 1) Product summary

A real-time Pong game service with:
- accounts & auth
- matchmaking and realtime play
- match records and ranking
- social/chat/admin features (as implemented in v1.0.0)

Post v1.0 focus:
- scale-out realtime reliability
- event-driven robustness
- observability/SRE readiness
- security/audit
- DR readiness

---

## 2) Core flows (must remain valid)

- Authenticated user can:
  - log in
  - start matchmaking
  - play realtime match
  - see match results/history
  - see ranking/stats (may be eventually consistent after v1.3)
- Operator/admin can:
  - investigate incidents via logs/metrics/traces (v1.2+)
  - audit sensitive actions (v1.5+)

---

## 3) Non-functional requirements

### 3.1 Reliability & correctness
- Realtime must have explicit reconnect and failure semantics.
- No “silent stuck” sessions.

### 3.2 Performance tracking (formalized in v1.2)
SLO format must be defined in v1.2 (values can evolve later):
- API latency p95
- WS connect success rate
- match start success rate
- tick loop jitter p95 (server-side)

### 3.3 Consistency model
- Match result persistence is strongly consistent (DB).
- Ranking/stats may be eventually consistent after v1.3.

### 3.4 Security & audit
- Auth parity across REST + WS.
- Sensitive actions are audited (v1.5).
- Rate limit and bot-abuse baseline required (v1.5).

### 3.5 Observability & operability
- logs + metrics + traces correlation
- alerts + runbooks + drills (v1.2)

### 3.6 DR readiness
- active-passive DR drills measured in v2.2.

---

## 4) Testing is a product requirement

If behavior cannot be proven by tests, it is incomplete.

Minimum:
- Unit + integration tests for backend changes
- Deterministic simulation tests for realtime loop changes
- WS integration tests for protocol/auth/reconnect changes
- Load tests + drills for scale/ops versions (per VERSIONING.md)
