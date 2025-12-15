package com.codexpong.backend.async.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/async/outbox/DeadLetterEventRepository.java
 * 설명:
 *   - DLQ에 적재된 이벤트를 조회/정리하기 위한 리포지토리다.
 */
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {
}
