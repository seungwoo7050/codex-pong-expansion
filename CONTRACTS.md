# CONTRACTS.md

This document defines how external contracts are stored, versioned, and validated.

External contracts are:
- REST API (OpenAPI)
- WebSocket protocol (handshake + event schemas + semantics)
- DB schema/migrations and public data semantics

---

## 1) Canonical locations

All canonical contracts live under `contracts/`:

- REST (OpenAPI):
  - `contracts/openapi.v1.0.0.yaml`
  - `contracts/openapi.v1.1.0.yaml` (only if v1.1 changes REST)
- WebSocket:
  - `contracts/ws.v1.0.0.md`
  - `contracts/ws.v1.1.0.md` (required because realtime architecture changes)
- DB schema summary (human-readable):
  - `contracts/db.v1.0.0.md`
  - `contracts/db.v1.1.0.md` (only if schema changes)

DB migrations remain the source of truth for schema execution, but the `contracts/db.*.md`
files must summarize key tables/relations and “publicly relied-on” semantics.

---

## 2) Contract change rule

If you change any contract:
1) Update the contract file in `contracts/` first.
2) Add/modify tests that prove the contract.
3) Implement code.
4) Ensure backwards compatibility unless VERSIONING explicitly allows breaking changes.

---

## 3) WebSocket contract must include (minimum)

- endpoint path(s)
- auth handshake (success/fail)
- message envelope (fields, required vs optional)
- event types and payload schema
- ordering assumptions (if any)
- ack/retry semantics (if any)
- reconnect semantics (timeouts/windows, rejoin rules)
- rate limits/backpressure behavior
- failure semantics (what happens if shard dies, etc.)

---

## 4) REST contract must include (minimum)

- auth mechanism and required headers/cookies
- error envelope schema
- endpoints and payload schemas

If OpenAPI is generated, the repo must still store an exported, stable file under `contracts/`.

---

## 5) DB contract summary must include (minimum)

- key tables and relations
- important indexes/constraints
- data ownership rules (what writes what)
- versioned behavior notes that consumers rely on (e.g., idempotency tables)

---

## 6) Validation requirements

- CI (or local scripts) MUST validate:
  - OpenAPI schema is syntactically valid
  - WS contract file exists for target version
  - DB migrations run on clean DB (integration test or script)
