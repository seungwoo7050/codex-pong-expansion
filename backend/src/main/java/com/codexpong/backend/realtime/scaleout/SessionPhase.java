package com.codexpong.backend.realtime.scaleout;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/SessionPhase.java
 * 설명:
 *   - 샤드 내 게임 세션의 주요 상태를 단순 표현한다.
 * 버전: v1.1.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-handoff-and-reconnect-semantics.md
 * 변경 이력:
 *   - v1.1.0: 상태 열거형 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public enum SessionPhase {
    WAITING,
    PLAYING,
    TERMINATED
}
