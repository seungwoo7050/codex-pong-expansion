package com.codexpong.backend.async.consumer;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/async/consumer/EventConsumptionRepository.java
 * 설명:
 *   - 이벤트 멱등성 기록을 보관한다.
 */
public interface EventConsumptionRepository extends JpaRepository<EventConsumption, EventConsumptionId> {
}
