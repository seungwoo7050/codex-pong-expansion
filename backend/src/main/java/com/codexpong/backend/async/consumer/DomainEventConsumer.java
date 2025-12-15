package com.codexpong.backend.async.consumer;

import com.codexpong.backend.async.event.DomainEvent;

/**
 * [인터페이스] backend/src/main/java/com/codexpong/backend/async/consumer/DomainEventConsumer.java
 * 설명:
 *   - 아웃박스 이벤트를 처리하는 소비자 계약을 정의한다.
 */
public interface DomainEventConsumer {

    String name();

    boolean supports(String eventType);

    void consume(DomainEvent event);
}
