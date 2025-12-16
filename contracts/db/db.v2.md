# db.v2.md

Service-owned schemas for v2.0 hybrid MSA. Each service owns its own schema and
credentials; cross-service SQL is forbidden. Migrations live under
`services/<svc>/db/migration/` and are applied independently per service.

## identity-service
- **Schema:** `identity_service`
- **DB User:** `identity_svc` (provisioned by ops/scripts with secrets via env)
- **Tables (authoritative):** identity_users, identity_sessions, identity_key_set,
  identity_key_audit
- **Access:** CRUD limited to identity-service. Other services interact via
  gRPC (`IdentityService`) only.

## match-service
- **Schema:** `match_service`
- **DB User:** `match_svc` (provisioned by ops/scripts with secrets via env)
- **Tables (authoritative):**
  - matches (external id + shard region + start time)
  - match_results (one-per-match with closure_reason, ended_at, trace_id)
  - match_player_results (per-player score/winner rows)
  - match_outbox (payload JSON + trace_id for continuity)
- **Access:** Only match-service writes/reads. Realtime shard persists match
  outcomes over gRPC; downstream consumers rely on outbox events (no direct
  SQL).

## chat-service
- **Schema:** `chat_service`
- **DB User:** `chat_svc` (provisioned by ops/scripts with secrets via env)
- **Tables (authoritative):** chat_channels, chat_members, chat_messages,
  chat_moderation_flags
- **Access:** Read/write only through chat-service. Edge-api calls gRPC to post
  and read chats; no other service queries `chat_service` tables directly.

## edge-api
- **Schema:** legacy modules only. New domains must be extracted to their
  owning service before writes occur.

## Observability + migrations
- Each service owns migrations under `services/<svc>/db/migration/` once code is
  extracted.
- Migrations must be reversible where feasible and run independently per
  schema.
