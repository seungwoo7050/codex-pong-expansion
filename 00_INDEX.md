# 00_INDEX.md

Entry point for humans and AI agents.

Assumption:
- v1.5 is implemented and stable.
- 2.x is the “scale-to-real-service” phase (hybrid MSA, event bus, DR, resilience, security/compliance, data/experimentation, edge/cost).
- 3.x is summarized only.

---

## Source of truth (priority order)

1) AGENTS.md
2) STACK_DESIGN.md
3) SERVICE_CATALOG.md
4) MIGRATION_PLAYBOOK.md
5) CONTRACTS.md
6) PRODUCT_SPEC.md
7) CODING_GUIDE.md
8) VERSIONING.md
9) BASELINE_1.5.md

If anything conflicts, follow the higher priority document.

---

## Directory map

- contracts/                 # external + internal + events + db summaries (canonical)
- services/                  # Spring Boot services (v2.0+)
- realtime/                  # gateway + shard (from v1.x)
- design/                    # design docs per version (English)
- runbooks/                  # operational docs (English)
- tools/loadtest/            # load tests
- tools/drills/              # drill scripts
- scripts/                   # canonical runners (tests/smoke/contracts/migration)

---

## Non-negotiable delivery gate (all versions)

No version is “done” unless:
- contracts are updated (when IO/behavior changes)
- tests are added/updated and pass
- cutover has rollback plan (v2.x)
- required runbooks/drills exist (v2.2+)
- results are reproducible using scripts/ and tools/

---

## Archive policy (context control)

Archive means MOVE, not delete.
Allowed to move to docs/archive/YYYYMMDD/:
- obsolete drafts, duplicated explanations, replaced roadmaps

Never archive:
- governance docs listed in “source of truth”
- anything under contracts/
- DB migrations

Each archive folder must contain README.md (what moved / why / what replaces it).
