# VERSION_PROMPTS.md

2.x 구현을 위한 에이전트 실행 지시문.
마이너 버전만 대상으로 한다(패치는 버그/리팩터링/문서/테스트만).

---

## 공통 규칙(모든 버전 적용)

- VERSIONING.md의 **단 하나의 마이너 버전**만 구현한다.
- 스택 드리프트 금지: Spring Boot / MariaDB / Redis / Nginx 고정.
- Kafka는 v2.1부터만 허용.
- v2.0+부터 cross-service DB query 금지.
- 계약 우선(Contract-first):
  - 동작/IO/스키마 의미가 바뀌면 contracts/**를 반드시 갱신하고 커밋에 포함한다.
  - OpenAPI는 코드에서 export로 생성해도 되지만, 생성 방법이 재현 가능해야 하고 결과 파일을 커밋해야 한다.
- 테스트 없이 완료 선언 금지. flaky 테스트 금지.

필수 읽기(순서):
00_INDEX.md → AGENTS.md → STACK_DESIGN.md → SERVICE_CATALOG.md → MIGRATION_PLAYBOOK.md →
CONTRACTS.md → PRODUCT_SPEC.md → CODING_GUIDE.md → VERSIONING.md → BASELINE_1.5.md

보고 형식(고정):
- 변경 요약
- 실행 방법
- 테스트 방법(scripts/ 경로 기반)
- (해당 시) 드릴/부하테스트 방법(tools/ 경로 기반)
- 롤백 방법(해당 시)

---

## v2.0 지시문 — 하이브리드 MSA 기반 구축

목표:
- 외부 REST/WS 계약은 기본적으로 유지
- 내부를 gRPC+Protobuf 기반 서비스로 단계적 분해
- 최소 3개 서비스: identity-service + match-service + chat-service를 “실제 데이터 소유권”으로 분리

반드시 할 일:
1) edge-api를 외부 REST 단일 진입점으로 고정(또는 기존 모놀리스를 edge-api로 전환).
2) identity-service / match-service / chat-service를 추출:
   - schema/migrations/DB 계정 분리
   - edge-api ↔ 서비스 간 gRPC 호출 구현
3) realtime-gateway는 access token을 identity 키셋으로 로컬 검증한다(스택/문서대로).
4) shard는 match 종료 시 match-service gRPC로 결과를 저장하게 한다.
5) contracts/services/*/v2.proto 및 contracts/db/db.v2.md를 “코드 기반으로” 작성/갱신하고 커밋한다.

테스트 게이트(필수):
- scripts/contract-test.sh (proto compile + openapi validation)
- 각 서비스 clean DB 마이그레이션 통합 테스트
- edge-api→gRPC→서비스 스모크 통합 테스트
- scripts/smoke.sh (login → match → history)
- traceId 연속성 검증 통합 테스트 1개 이상
- (데이터 이동이 있으면) scripts/migration-test.sh

필수 문서:
- design/backend/v2.0-service-boundaries.md
- design/backend/v2.0-grpc-contracts.md
- runbooks/v2.0-cutover-and-rollback.md

---

## v2.1 지시문 — Kafka 이벤트 버스 도입 + 운영급 컨슈머

목표:
- Kafka 도입
- outbox→relay→Kafka publish
- at-least-once + 멱등성 + retry/DLQ 운영까지 완성

반드시 할 일:
1) contracts/events/envelope + match-ended 스키마를 코드 기반으로 작성/갱신 후 커밋.
2) 토픽/소유권/consumer group 규칙을 design 문서로 고정.
3) DLQ + 재처리 절차(runbook) 포함.

테스트 게이트(필수):
- Kafka Testcontainers 통합 테스트:
  - 중복 전달 → 멱등성 유지
  - consumer 재시작 → 중복 적용 없음
  - retry → DLQ 라우팅
- broker 장애 시나리오(drill) 문서화
- lag/DLQ 지표 및 알림 반영(가능 범위에서)

필수 문서:
- design/backend/v2.1-kafka-topics-and-envelope.md
- runbooks/v2.1-dlq-and-lag-ops.md

---

## v2.2 지시문 — 멀티리전/DR(Active-Passive)

목표:
- active-passive DR을 “리허설 + 수치(RTO/RPO)”로 증명
- region-fixed realtime 전제에서 control/data plane 분리

반드시 할 일:
- DR 대상 최소 1개 critical path를 명시(권장: identity + match persistence).
- failover/rollback 절차를 runbook으로 고정하고 실행 가능하게 만든다.
- 측정값(RTO/RPO)을 남긴다.

게이트(필수):
- DR drill runbook + 결과 보고서(측정값 포함)
- 데이터 무결성 체크리스트 포함

필수 문서:
- design/infra/v2.2-multi-region-architecture.md
- runbooks/v2.2-dr-drills.md

---

## v2.3 지시문 — Resilience(연쇄 장애 방지)

목표:
- 타임아웃/재시도 폭탄/부분 장애로 인한 연쇄 장애를 차단

반드시 할 일:
- gRPC 타임아웃/재시도 정책을 명시적 config로 통일
- 서킷브레이커/벌크헤드/로드셰딩 정책 적용
- 장애 주입 드릴 최소 2개(다운스트림 타임아웃, DB 풀 고갈 등)

게이트(필수):
- resilience 통합 테스트(재시도 bounded, CB open, 외부 에러 포맷 안정)
- tools/drills/ + runbooks/에 재현 가능한 드릴 2개 이상

필수 문서:
- design/sre/v2.3-resilience-standards.md
- runbooks/v2.3-chaos-drills.md

---

## v2.4 지시문 — 보안/컴플라이언스 하드닝

목표:
- 대형 서비스 기본 보안 체계를 갖춘다

반드시 할 일:
- 서비스 간 인증(mTLS 또는 서명 토큰) + 권한 모델
- 시크릿/키 관리 및 롤오버 정책
- PII 정책(분류/마스킹/보관/필요 시 암호화)
- 공급망 보안(SCA/이미지 스캔/SBOM) 최소 CI 게이트

게이트(필수):
- authn/z 일관성 테스트
- 감사로그 커버리지 유지
- 보안 CI 산출물 생성 및 정책 기반 fail

필수 문서:
- design/security/v2.4-security-baseline.md
- runbooks/v2.4-key-rotation-and-incident.md

---

## v2.5 지시문 — 데이터 플랫폼 & 실험(성장 기능)

목표:
- 분석/실험/피처플래그를 운영 가능하게 만든다

제약:
- “새로운 대형 런타임” 추가 없이 수행(분석 저장은 MariaDB analytics schema 또는 이벤트 파일로 제한).

반드시 할 일:
- 분석 이벤트 스키마 정의 및 검증(코드 기반으로 contracts/events 갱신)
- feature flag 운영(롤아웃/롤백)
- A/B 할당 + exposure logging
- 프라이버시 제약(샘플링/마스킹/보관)

게이트(필수):
- 이벤트 필수 필드/스키마 검증 테스트
- exposure 중복 방지/추적성 테스트
- 마스킹/샘플링 검증

필수 문서:
- design/backend/v2.5-analytics-and-experiments.md
- runbooks/v2.5-feature-flag-ops.md

---

## v2.6 지시문 — 엣지/스케일/비용 규율

목표:
- CDN/WAF/레이트리밋/지속 부하테스트/용량모델/비용관측까지 대형 서비스 방식으로 고정

반드시 할 일:
- ingress/nginx 캐시 정책(키/무효화/안전 규칙) 문서화 + 검증
- WAF/봇 방어/레이트리밋 표준화
- 부하테스트 3종 실행 가능하게(tools/loadtest/)
- 용량 모델 + 결과(runbook)에 기록
- 서비스/토픽/리전 단위 비용 신호(메트릭) 정의

게이트(필수):
- tools/loadtest/ 실행 가능 + 임계치(최소 기준) 문서화
- 엣지 장애 드릴 1개 이상(runbook)

필수 문서:
- design/infra/v2.6-edge-cache-waf.md
- runbooks/v2.6-loadtest-and-capacity.md

---

## 3.x 요약(상세 실행 지시 없음)

- v3.0: 셀 아키텍처 도입(블라스트 레디우스 축소)
- v3.1: 컨트롤 플레인 일부 active-active(읽기 중심)
- v3.2: 데이터/실험/프라이버시 자동화 성숙
