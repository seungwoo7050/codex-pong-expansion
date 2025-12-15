# v0.12.0 잡 진행률 WebSocket 계약

## 1. 연결 정보
- 엔드포인트: `ws://<호스트>:8080/ws/jobs`
- 프로토콜: **raw WebSocket** (STOMP 미사용)
- 인증: 쿼리 파라미터 `token=<JWT>` 필수. `WebSocketAuthHandshakeInterceptor`가 JWT를 파싱해 사용자 정보를 세션 속성에 저장한다. 토큰이 없거나 유효하지 않으면 핸드셰이크가 거부되고 세션은 생성되지 않는다.
- 실패 동작: 인증되지 않은 연결은 인터셉터 단계에서 `false`를 반환해 거부되며, 핸들러 진입 후에도 사용자 정보가 없으면 `CloseStatus.NOT_ACCEPTABLE(선택 이유: "인증이 필요합니다.")`로 즉시 종료된다.
- 초기 메시지: 연결이 성공하면 서버가 `{ "type": "job.connected" }` 텍스트 메시지를 1회 전송해 구독 준비 상태를 알린다.

## 2. 메시지 포맷
- 공통 envelope: `{ "type": <이벤트 타입 문자열>, "payload": <이벤트별 객체> }`
- 직렬화: Jackson을 통한 JSON 텍스트 메시지.
- 필드 정의 (코드 기준):
  - JobProgressMessage: `jobId(Long)`, `progress(int, 0~100)`, `phase(String)`, `message(String)`
  - JobResultMessage: `jobId(Long)`, `status(JobStatus: COMPLETED/FAILED)`, `resultUri(String)`, `checksum(String)`, `errorCode(String)`, `errorMessage(String)`
  - 퍼블리시 시에는 `JobEventPublisher`가 envelope `payload`를 재구성해 아래 스키마로 송신한다.

## 3. 이벤트 타입 및 스키마 (v0.12.0)
### 3.1 진행률: `job.progress`
- 발생 조건: Redis Streams에서 진행률 메시지(`JobProgressMessage`)를 수신해 해당 사용자 세션에 중계할 때.
- Payload 필드
  - `jobId` (Long): 잡 식별자
  - `progress` (int): 0~100 진행률
  - `phase` (String): 워커 단계(예: PREPARE, ENCODE 등 자유 문자열)
  - `message` (String): 사람이 읽을 로그/상태 메시지
- 샘플
```json
{
  "type": "job.progress",
  "payload": {
    "jobId": 42,
    "progress": 55,
    "phase": "ENCODE",
    "message": "ffmpeg 진행 중"
  }
}
```

### 3.2 완료: `job.completed`
- 발생 조건: `JobResultMessage`가 `status=COMPLETED`로 전달될 때.
- Payload 필드
  - `jobId` (Long)
  - `downloadUrl` (String): `/api/jobs/{jobId}/result` 형태의 다운로드 엔드포인트
  - `checksum` (String): 결과 파일 체크섬
- 샘플
```json
{
  "type": "job.completed",
  "payload": {
    "jobId": 42,
    "downloadUrl": "/api/jobs/42/result",
    "checksum": "abc123sha"
  }
}
```

### 3.3 실패: `job.failed`
- 발생 조건: `JobResultMessage`가 `status=FAILED`로 전달될 때.
- Payload 필드
  - `jobId` (Long)
  - `errorCode` (String): 서버/워커 정의 에러 코드
  - `errorMessage` (String): 사용자에게 노출 가능한 오류 설명
- 샘플
```json
{
  "type": "job.failed",
  "payload": {
    "jobId": 42,
    "errorCode": "ENCODE_TIMEOUT",
    "errorMessage": "인코딩 타임아웃"
  }
}
```

## 4. 클라이언트 동작 가이드
- 재연결: 서버가 별도 재전송을 보장하지 않으므로 클라이언트는 연결 끊김 시 지수 백오프 기반 재시도를 권장하며, 필요 시 REST `GET /api/jobs/{jobId}`로 상태를 보강한다.
- 서버 종료/에러: 인증 실패 시 즉시 종료(`NOT_ACCEPTABLE`), 세션 종료 시 추가 이벤트 없이 연결만 끊긴다. 서버는 동일 이벤트를 중복 송신할 수 있으므로 클라이언트는 `jobId` 단위로 최종 상태를 멱등 처리한다.

## 5. 설계 불일치 메모
- `design/realtime/v0.12.0-job-progress-events.md`에서는 이벤트가 `type + 필드` 평평한 구조로 표기되어 있지만, 실제 구현은 `type` + `payload` envelope를 사용한다. 모바일 계약은 코드 구현(`JobEventPublisher`)을 기준으로 한다.
