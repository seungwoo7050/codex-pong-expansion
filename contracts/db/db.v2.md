# db.v2.md

Service-owned schemas for v2.0 hybrid MSA. Each service owns its own schema and
credentials; cross-service SQL is forbidden.

## identity-service
- **Schema:** `identity_` (per-service DB user)
- **Tables (authoritative):** users, credentials, sessions/tokens, key_set_audit
- **Access:** CRUD limited to identity-service. Other services interact via
  gRPC (`IdentityService`) only.

## match-service
- **Schema:** `match_`
- **Tables (authoritative):** matches, match_results, player_results,
  match_outbox
- **Access:** Only match-service writes/reads. Realtime shard persists match
  outcomes over gRPC; downstream consumers rely on outbox events (no direct
  SQL).

## chat-service
- **Schema:** `chat_`
- **Tables (authoritative):** channels, channel_members, messages,
  moderation_flags
- **Access:** Read/write only through chat-service. Edge-api calls gRPC to post
  and read chats; no other service queries `chat_` tables directly.

## edge-api
- **Schema:** legacy modules only. New domains must be extracted to their
  owning service before writes occur.

## Observability + migrations
- Each service owns migrations under `services/<svc>/db/migration/` once code is
  extracted.
- Migrations must be reversible where feasible and run independently per
  schema.
