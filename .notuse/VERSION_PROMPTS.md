# VERSION_PROMPTS.md

각 버전별 AI 에이전트 실행 지시문 모음.
이 문서는 “스코프 통제 + 테스트 강제”가 목적이다.

---

## 공통 지시(모든 버전에 적용)

당신은 VERSIONING.md에 정의된 **단 하나의 버전**만 구현한다. (스코프 확장 금지)

반드시 읽을 것(순서):
1) 00_INDEX.md
2) AGENTS.md
3) STACK_DESIGN.md
4) PRODUCT_SPEC.md
5) CODING_GUIDE.md
6) VERSIONING.md
7) BASELINE_1.0.md
8) 해당 버전의 design/** 문서(한국어)
9) CLONE_GUIDE.md(있다면)

작업 절차(고정):
1) 타겟 버전을 명확히 선언한다.
2) 변경 목록(코드/테스트/문서)을 계획으로 먼저 정리한다.
3) 코드 구현(주석은 한국어).
4) 테스트 구현(필수).
5) 테스트 실행 후 녹색이 될 때까지 수정한다.
6) VERSIONING.md가 요구하는 한국어 설계 문서를 작성/갱신한다.
7) 결과로 다음을 짧게 보고한다:
   - 변경 요약
   - 실행 방법
   - 테스트 방법
   - (해당 시) 부하테스트/드릴 실행 방법

절대 규칙(중요):
- **테스트 없이 “완료” 선언 금지.**
- flaky 테스트 금지(실시간 로직에 sleep 기반 금지).
- 외부 계약(REST/WS/DB)을 변경하면 계약 문서를 먼저 업데이트하고 테스트로 고정한다.
- STACK_DESIGN.md의 금지 사항(스택 드리프트) 위반 금지.

---

## v1.1.0 지시문 – 실시간 수평 확장 (Gateway + Shard + State/Handoff)

목표:
- WS 접속 유지 전용의 Stateless Gateway
- 권위(authoritative) Game Session Shard의 수평 확장
- Redis 기반 라우팅/상태 스토어/핸드오프
- 재접속 및 장애 시 “복구/종료” 규칙이 명시되고 테스트로 검증됨

필수 산출물:
- realtime-gateway (독립 실행 단위)
- game-session-shard (독립 실행 단위)
- Redis에 다음 기능 구현:
  - shard registry(heartbeat 포함)
  - session -> shard 소유권 매핑
  - snapshot(또는 종료 근거 데이터) 저장
  - gateway<->shard 메시징(Streams 또는 Pub/Sub 중 하나를 설계 문서에서 확정)

핸드오프 정책(둘 중 하나를 **명시적으로 확정**):
- (A) Resume: snapshot 기반 복구
- (B) Deterministic terminate: 규칙 기반 종료(악용 방지 포함)

테스트 게이트(필수):
- 게임 루프 결정적 시뮬레이션 테스트(시간 주입/스텝 방식, sleep 금지)
- WS 통합 테스트:
  - 인증 실패 케이스
  - 재접속 경로
- 장애 시나리오 테스트(자동화 스크립트 또는 테스트 하네스):
  - 진행 중 shard kill -> 정책대로 “복구/종료” 검증
- 부하테스트(최소 1개):
  - WS connect storm
  - steady-state 메시지 처리

필수 문서(한국어):
- design/realtime/v1.1.0-gateway-shard-protocol.md
- design/realtime/v1.1.0-handoff-and-reconnect-semantics.md
- design/infra/v1.1.0-realtime-topology.md

---

## v1.2.0 지시문 – Observability & SRE

목표:
- “대시보드 있음”이 아니라 운영 가능 상태(SLO/알림/런북/드릴)
- 로그/메트릭/트레이스 상관관계(traceId) 기본

필수 산출물:
- OpenTelemetry 기반 트레이싱(최소: api/gateway/shard/worker)
- SLO/SLI 정의 + 알림 규칙 + 런북 링크
- 핵심 메트릭(최소):
  - WS 연결 성공률
  - 매치 시작 성공률
  - API p95 latency
  - tick loop jitter p95(서버 기준)

테스트/드릴 게이트(필수):
- trace 상관관계가 end-to-end로 이어지는 통합 테스트 1개 이상
- 재현 가능한 드릴 3종(스크립트 포함):
  1) DB 장애/지연
  2) Redis 장애/지연
  3) gateway/shard kill

필수 문서(한국어):
- design/sre/v1.2.0-slo-alert-runbook.md
- design/backend/v1.2.0-otel-tracing.md
- runbooks/v1.2.0-drills.md

---

## v1.3.0 지시문 – 비동기 & 데이터 견고성 (Outbox/Consumer/DLQ)

목표:
- 매치 종료 -> 결과 이벤트 발행 -> 랭킹/통계/알림/탐지 비동기 처리
- 재시도/DLQ/멱등성/Outbox로 운영급 신뢰성

필수 산출물:
- MariaDB Outbox(도메인 write와 동일 트랜잭션)
- Relay publisher(재시도 정책)
- Consumers(최소):
  - ranking
  - stats
  - notifications
  - abuse signals(간단 휴리스틱)
- DLQ + bounded retry + idempotency(eventId 기반)

테스트 게이트(필수):
- outbox 트랜잭션 결합 테스트
- 중복 이벤트 전달 시 멱등성 테스트
- retry -> DLQ 라우팅 테스트
- DLQ 재처리/폐기 운영 절차 문서(한국어)

필수 문서(한국어):
- design/backend/v1.3.0-outbox-and-consumers.md
- runbooks/v1.3.0-dlq-ops.md

---

## v1.4.0 지시문 – Platformization (K8s + IaC + GitOps)

목표:
- 재현 가능한 배포 + 무중단/안전 롤아웃

필수 산출물:
- local K8s(kind/minikube) 배포 경로
- Terraform baseline
- GitOps(ArgoCD 등)
- readiness/liveness + graceful shutdown(특히 gateway)

테스트 게이트(필수):
- local K8s에서 문서화된 커맨드로 배포/업데이트 성공
- 롤링 업데이트 중 재접속 정책이 깨지지 않는 시나리오 검증

필수 문서(한국어):
- design/infra/v1.4.0-k8s-iac-gitops.md

---

## v1.5.0 지시문 – Security & Audit

목표:
- OAuth 동의/스코프/감사 로그/관리자 액션 로그
- 레이트리밋/봇 방어 baseline

필수 산출물:
- 감사 로그(append-only 정책)
- 관리자 액션 감사(누락 금지)
- 레이트리밋 정책(login/chat/ws) + 관측(메트릭/로그)
- OAuth consent/scope(사용 중이면)

테스트 게이트(필수):
- 민감 액션 감사 로그 기록 테스트
- 레이트리밋 enforcement 테스트
- REST/WS 인증 일관성 테스트

필수 문서(한국어):
- design/security/v1.5.0-audit-and-rate-limit.md

---

## v2.x 요약 지시

v2.0.0:
- 서비스 경계 분리 + 계약 테스트 필수

v2.1.0:
- Kafka/PubSub 도입(허용)
- exactly-once 금지, at-least-once + 멱등성만

v2.2.0:
- Active-passive DR부터
- DR 리허설(RTO/RPO 측정) + 런북 필수
