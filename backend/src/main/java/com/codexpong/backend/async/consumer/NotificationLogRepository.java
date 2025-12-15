package com.codexpong.backend.async.consumer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/async/consumer/NotificationLogRepository.java
 * 설명:
 *   - 알림 발송 기록을 보관한다.
 */
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Optional<NotificationLog> findByEventIdAndUserId(String eventId, Long userId);
}
