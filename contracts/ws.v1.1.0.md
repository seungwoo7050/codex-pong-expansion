# ws.v1.1.0 – 게이트웨이/샤드 분리 프로토콜

## 목적
- v1.1.0 실시간 수평 확장 토폴로지에서 게이트웨이와 샤드 사이, 그리고 클라이언트와 게이트웨이 사이의 WebSocket 계약을 정의한다.
- 결정적 종료 정책(B)을 전제로 재접속/장애 시 동작을 명시한다.

## 엔드포인트
- 클라이언트 → 게이트웨이: `ws://<gateway>/ws/game?sessionId={sessionId}&token={jwt}`
- 게이트웨이 ↔ 샤드: Redis Streams 기반 `game:request:{shardId}` / `game:response:{sessionId}`

## 인증 핸드셰이크
- 쿼리 파라미터 `token`은 JWT로, 게이트웨이는 토큰 유효성만 검증하고 상태를 저장하지 않는다.
- 검증 실패 시 즉시 종료(`4401 AUTH_FAILURE`), 종료 컨텍스트는 Redis에 `termination:{sessionId}`로 기록한다.

## 메시지 envelope (공통)
- `messageId`(uuid), `type`, `occurredAt`(iso8601), `sessionId`, `traceId`, `payload`(객체)

## 주요 타입
- 클라이언트 → 게이트웨이
  - `CLIENT_INPUT`: `{"direction":"LEFT|RIGHT|NONE"}`
  - `RECONNECT`: 재접속 시 빈 payload
- 게이트웨이 → 샤드
  - `SESSION_CONNECTED`: `{token}`
  - `SESSION_RECONNECTED`: `{}`
  - `FORWARD_INPUT`: `{direction}`
- 샤드 → 게이트웨이
  - `SESSION_ACK`: `{}`
  - `PLAY_STARTED`: `{}`
  - `STATE_SNAPSHOT`: `{ball:{x,y},paddle:{left,right}}`
  - `TERMINATED`: `{reason}`
- 게이트웨이 → 클라이언트
  - 위 샤드 응답을 그대로 전달하며, `TERMINATED` 수신 시 소켓을 닫는다.

## 재접속 규칙
- 재접속 윈도우: `gateway.reconnect.windowSeconds` 기본 10초.
- 샤드가 활성(heartbeat 유효)하고 종료 컨텍스트가 없으면 `SESSION_RECONNECTED`를 샤드에 전송 후 클라이언트에 `SESSION_ACK` 전파.
- 종료 컨텍스트가 존재하거나 샤드 heartbeat가 끊긴 경우 즉시 `TERMINATED{reason=SHARD_UNAVAILABLE}` 전파 후 소켓 종료.

## 장애/종료 코드
- `AUTH_FAILURE`: 토큰 검증 실패
- `SHARD_UNAVAILABLE`: 소유 샤드 heartbeat 소실 혹은 매핑 불가
- `NORMAL_COMPLETION`: 게임 종료
- `POLICY_TERMINATED`: 악용 방지 정책에 따른 종료

## 순서/전달 보장
- Streams 소비는 샤드 단위 `XGROUP` 기반 at-least-once.
- 메시지 타입별 idempotent 처리 필요(`messageId` 기반).

## 백프레셔/속도 제한
- 게이트웨이는 세션당 미확인 메시지 큐 길이가 임계치를 넘으면 `4208 RATE_LIMIT`로 종료한다.

## 테스트 요구사항 추적
- `GatewayShardScaleOutTest`에서 인증 실패/재접속/샤드 장애 종료를 결정적으로 검증한다.
- 별도 WS 통합 테스트는 추후 실제 게이트웨이 프로세스가 추가되면 확장한다.
