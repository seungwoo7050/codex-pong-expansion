package com.codexpong.backend.realtime.scaleout;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/TerminationReason.java
 * 설명:
 *   - 결정적 종료 정책에서 사용되는 표준 종료 사유 코드를 정의한다.
 * 버전: v1.1.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-handoff-and-reconnect-semantics.md
 * 변경 이력:
 *   - v1.1.0: 종료 사유 열거형 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public enum TerminationReason {
    AUTH_FAILURE,
    SHARD_UNAVAILABLE,
    NORMAL_COMPLETION,
    POLICY_TERMINATED
}
