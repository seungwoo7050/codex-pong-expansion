# SERVICE_CATALOG.md

Service boundaries, owned data, and interaction model for 2.x.

This file defines “what exists and who owns what”.
Detailed design lives under design/**.

---

## edge-api (external boundary + strangler host)

Purpose:
- preserve external REST contract for clients
- orchestrate internal gRPC calls
- host remaining “legacy modules” until extracted (strangler pattern)

DB ownership:
- edge-api may temporarily own legacy schemas for not-yet-extracted modules.
- edge-api MUST NOT read/write other services’ schemas.

Key rules:
- external error envelope is produced here
- traceId/correlationId must be propagated to all downstream calls

---

## identity-service

Owns:
- identity_* schema (users, credentials, sessions/tokens, OAuth grants/consent, security logs)

Responsibilities:
- issue/validate access tokens (issuer)
- provide verification keys (key-set endpoint)
- manage OAuth grants/consent lifecycle (if applicable)

Sync APIs (gRPC):
- token validation (optional; local validation is default)
- user profile minimal queries

---

## match-service (authoritative match persistence)

Owns:
- match_* schema (matches, results, history, outbox)

Responsibilities:
- persist match results (single source of truth)
- write MatchEnded outbox event (authoritative producer)

Sync APIs (gRPC):
- persistMatchResult(matchId, players, score, reason, timestamps, traceId, ...)
- queryMatchHistory(userId, paging, ...)

---

## chat-service

Owns:
- chat_* schema (channels, messages, moderation state)

Responsibilities:
- chat persistence and retrieval
- produce moderation/abuse signals (event-driven from v2.1)

---

## matchmaking-service (eventually extracted)

Owns (when extracted):
- mm_* schema (tickets, queues, assignments)

Responsibilities:
- matchmaking queue logic
- assigns match/session and coordinates with realtime components

Note:
- in v2.0, matchmaking can remain inside edge-api as “legacy module” if not extracted yet.

---

## rating-service (best after v2.1)

Owns:
- rating_* schema (ratings, ranks, aggregates)

Responsibilities:
- consume MatchEnded and update ranking/statistics (event-driven)

---

## replay-worker

Owns:
- replay_* schema (jobs, artifacts, status)

Responsibilities:
- async exports / replays
- consumes MatchEnded optionally

---

## realtime services (existing from v1.x)

- realtime-gateway:
  - WS connection management (stateless)
  - validates access tokens locally using identity keys
  - routes client input to shard
- game-session-shard:
  - authoritative tick loop
  - on match completion: calls match-service gRPC to persist result
  - must propagate traceId/correlationId

State substrate:
- Redis is used for coordination/routing/session mappings per existing realtime design.
