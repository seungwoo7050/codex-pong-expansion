package com.codexpong.backend.async.event;

/**
 * [상수] backend/src/main/java/com/codexpong/backend/async/event/OutboxEventType.java
 * 설명:
 *   - 아웃박스 이벤트 타입 문자열을 모아둔다.
 */
public final class OutboxEventType {

    public static final String MATCH_RESULT_RECORDED = "MATCH_RESULT_RECORDED";

    private OutboxEventType() {
    }
}
