# CONTRACTS.md

Contract storage, compatibility policy, and validation rules.

Contracts are canonical. Code must conform to contracts, not the other way around.
However, some contract artifacts may be generated from code (e.g., OpenAPI export),
and those generated artifacts are still canonical once committed.

---

## 1) Canonical structure

contracts/
  external/
    openapi.v1.5.yaml
    ws.v1.5.md
    errors.md
    auth.md
  services/                   # gRPC contracts (v2.0+)
    identity/v2.proto
    match/v2.proto
    chat/v2.proto
    matchmaking/v2.proto
    rating/v2.proto
  events/                     # event schemas (v2.1+)
    envelope.v1.md
    match-ended.v1.json
    abuse-signal.v1.json
    rank-updated.v1.json
  db/
    db.v1.5.md
    db.v2.md

---

## 2) Contract-first rule (how it works in practice)

If behavior changes any of:
- external REST/WS
- internal gRPC
- event payload schemas
- public DB semantics

then contracts/** MUST be updated and committed in the same change set.

### Generated artifacts rule (OpenAPI)
- REST contract is stored as OpenAPI YAML under contracts/external/.
- It may be exported from code, but:
  - export must be deterministic and documented,
  - the exported file must be committed,
  - and tests must validate it (scripts/contract-test.sh).

### Hand-authored contracts rule (WS / gRPC / events)
- WS contract markdown, proto files, and event schema files are hand-authored.
- Agents may derive them from existing code/behavior, but the contract file becomes the canonical truth.

---

## 3) Compatibility policies

### External REST/WS
- Backward compatible by default.
- Breaking changes require:
  - explicit VERSIONING scope,
  - migration/deprecation notes,
  - and e2e coverage.

### Protobuf (internal)
Allowed (non-breaking):
- add optional fields
- add new messages / new RPCs

Breaking:
- removing fields
- reusing field tags with new meaning
- changing field types/semantics without a new version and deprecation plan

---

## 4) Event envelope policy (v2.1+)

All events MUST include:
- eventId (idempotency key)
- eventType
- schemaVersion
- occurredAt
- traceId/correlationId
- producer (service name)
- payload (validated)

Reliability stance:
- at-least-once + idempotent consumers
- no exactly-once claims

---

## 5) Validation requirements (scripts/CI)

Repo MUST provide:
- scripts/contract-test.sh
  - validate OpenAPI YAML syntax
  - compile all protos
  - ensure required event schema files exist (and are structurally valid)

- scripts/migration-test.sh
  - boot key services with clean DB schemas
  - run minimal gRPC smoke between edge-api and at least one service
