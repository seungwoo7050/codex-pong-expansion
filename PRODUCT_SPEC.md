# PRODUCT_SPEC.md

Product requirements for 2.x growth (post v1.5 baseline).

---

## 1) User-visible invariants (must remain true)

- login works reliably
- matchmaking starts a match
- realtime match completes deterministically
- match results are persisted and visible
- ranking/statistics may be eventually consistent
- audit visibility for sensitive/admin actions remains available

---

## 2) System invariants (2.x)

- no cross-service DB queries
- clear data ownership per service
- internal synchronous calls via gRPC
- async via events (v2.1+)
- observability continuity (trace correlation end-to-end)
- safe rollouts with rollback plan (feature flags/routing toggles)

---

## 3) Security & compliance growth (v2.4)

Must be achieved by v2.4:
- service-to-service authentication and authorization model
- secrets/key rotation policy
- PII policy:
  - classification, masking, retention
  - encryption for sensitive fields where applicable
- supply-chain security baseline (documented + CI gate)

---

## 4) Data/experimentation growth (v2.5)

Must be achieved by v2.5:
- analytics event pipeline (schema-defined, validated)
- feature flags with safe rollout/rollback
- experiment assignment + exposure logging
- privacy constraints enforced for analytics (masking/sampling/retention)

---

## 5) Edge/scale/cost growth (v2.6)

Must be achieved by v2.6:
- caching policy (CDN-equivalent behavior documented, implemented via ingress/nginx rules)
- WAF/rate limit/bot baseline at edge + app
- continuous load test baseline + capacity model
- cost observability signals per service/topic/region (metrics-level)

---

## 6) Testing is a product requirement

If behavior cannot be proven by tests, it is incomplete.
