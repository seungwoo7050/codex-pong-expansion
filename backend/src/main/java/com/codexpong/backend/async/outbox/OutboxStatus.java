package com.codexpong.backend.async.outbox;

/**
 * [열거형] backend/src/main/java/com/codexpong/backend/async/outbox/OutboxStatus.java
 * 설명:
 *   - 아웃박스 이벤트의 상태를 표현한다.
 *   - PENDING: 아직 릴레이되지 않음
 *   - PUBLISHED: 모든 소비자가 성공 처리함
 *   - FAILED: 재시도 한계를 넘어 DLQ로 이동함
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
