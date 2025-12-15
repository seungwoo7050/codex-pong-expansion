# 00_INDEX.md

This repo is a real-time Pong game service. This file is the entrypoint for humans and AI coding agents.

---

## Source of truth (priority order)

1) `AGENTS.md`
2) `STACK_DESIGN.md`
3) `CONTRACTS.md`   (NEW)
4) `PRODUCT_SPEC.md`
5) `CODING_GUIDE.md`
6) `VERSIONING.md`
7) `BASELINE_1.0.md`

If anything conflicts, follow the higher priority document.

---

## Language policy

- All repository docs: **English**
- Code comments: **English**
- (Exception) Prompt files may be Korean. (Not covered in this rewrite.)

---

## What is already done?

- `v1.0.0` is implemented.
- The minimal “do not break” facts about v1.0.0 are in `BASELINE_1.0.md`.
- The canonical external contracts are in `contracts/` (defined in `CONTRACTS.md`).

---

## Where to put version design and ops docs?

- Design docs: `design/`
  - `design/infra/`, `design/backend/`, `design/realtime/`, `design/sre/`, `design/security/`
- Runbooks: `runbooks/`
- Load tests: `tools/loadtest/`
- Drill scripts: `tools/drills/`
- Contracts: `contracts/`

---

## Post v1.0.0 roadmap (reordered by priority)

- v1.1.0 Realtime Scale-out (Gateway + Shard + State/Handoff)
- v1.2.0 Observability & SRE (SLO/alerts/runbooks/drills + trace correlation)
- v1.3.0 Async & Data Robustness (Outbox + consumers + DLQ/retry/idempotency)
- v1.4.0 Platformization (K8s + IaC + GitOps + rollout safety)
- v1.5.0 Security & Audit (OAuth consent/scope, audit logs, rate limit/bot baseline)
- v2.0.0 Service Decomposition (hybrid MSA boundaries + contract tests)
- v2.1.0 Event Bus (Kafka/PubSub, at-least-once + idempotency)
- v2.2.0 Multi-region & DR (active-passive + DR drills)

---

## Non-negotiable: Testing gate

No version is “done” unless:
- tests are added/updated and pass
- changes are deterministic (no flaky sleep-based realtime tests)
- load tests/drills exist where required by VERSIONING.md

---

## Archiving policy (to keep context small)

- Archive means **move**, not delete.
- Allowed to move to `docs/archive/YYYYMMDD/`:
  - obsolete drafts, long historical notes, replaced roadmaps, duplicated explanations
- Never archive:
  - `AGENTS.md`, `STACK_DESIGN.md`, `CONTRACTS.md`, `PRODUCT_SPEC.md`, `CODING_GUIDE.md`, `VERSIONING.md`, `BASELINE_1.0.md`
  - anything under `contracts/`
  - DB migrations
- Every archive folder MUST include `README.md` with:
  - what was moved
  - why it is archived
  - what replaces it (file path)
