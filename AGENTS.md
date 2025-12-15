# AGENTS.md

Rules for AI coding agents working in this repository.

---

## 1) Mandatory reading order

1) 00_INDEX.md
2) AGENTS.md
3) STACK_DESIGN.md
4) SERVICE_CATALOG.md
5) MIGRATION_PLAYBOOK.md
6) CONTRACTS.md
7) PRODUCT_SPEC.md
8) CODING_GUIDE.md
9) VERSIONING.md
10) BASELINE_1.5.md

---

## 2) Hard constraints

### 2.1 One minor version per change set
A change set MUST target exactly one minor version in VERSIONING.md (e.g., v2.1).
Mixed-version diffs are invalid; propose split commits.

### 2.2 No stack drift
Unless explicitly approved:
- do NOT replace Spring Boot
- do NOT replace MariaDB as the primary DB
- do NOT introduce Kafka before v2.1
- do NOT introduce active-active multi-region before v3.x
- do NOT allow cross-service DB queries (v2.0+)
- do NOT add new runtime components not listed in STACK_DESIGN.md for the target version

### 2.3 Contract-first, but “generated artifacts” are allowed
Contracts are canonical under contracts/**.

Rules:
- If behavior changes any contract (external REST/WS, internal gRPC, event schema, DB semantics):
  - contracts/** MUST be updated and committed in the same change set.
- If a contract file is generated from code (e.g., OpenAPI export):
  - generation must be deterministic and documented (scripts/contract-test.sh),
  - and the generated file must still be committed.

### 2.4 Data ownership (v2.0+)
- Each service owns its schema + migrations.
- No cross-service DB reads/writes. Ever.
- Derived data must come from events or explicit APIs, not cross-DB joins.

### 2.5 No binary artifacts in git
- gradle-wrapper.jar may be used locally but MUST NOT be committed.
- Any large/binary artifact must be ignored or stored outside git.

---

## 3) Testing gate (non-negotiable)

Minimum expectations by version:
- v2.0: contract tests + integration tests + migration tests (if data moved) + e2e smoke
- v2.1: duplicate delivery + consumer restart + retry->DLQ tests
- v2.2: DR drill procedures + measured results (RTO/RPO)
- v2.3: resilience tests + fault injection drills
- v2.4: security/compliance validation + key/secrets procedures
- v2.5: analytics/experimentation correctness + privacy checks
- v2.6: load tests + capacity baseline + edge failure drill

Flaky tests are unacceptable.

---

## 4) Standard workflow (must follow)

1) Declare the target minor version.
2) Plan: contracts + code + tests + docs + runbooks/drills.
3) Update contracts/** (or define generation) for changed behaviors.
4) Implement code.
5) Implement tests.
6) Run tests and fix until green.
7) Write/update required design/runbooks listed in VERSIONING.md.
8) Report: what changed / how to run / how to test / how to drill / how to rollback (if applicable).
