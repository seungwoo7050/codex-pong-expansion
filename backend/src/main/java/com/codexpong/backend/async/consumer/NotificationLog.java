package com.codexpong.backend.async.consumer;

import com.codexpong.backend.common.KstDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/async/consumer/NotificationLog.java
 * 설명:
 *   - 알림 소비자가 발송된 메시지를 기록한다.
 *   - eventId+userId로 중복을 방지한다.
 */
@Entity
@Table(name = "notification_logs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"eventId", "userId"})
})
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String eventId;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected NotificationLog() {
    }

    public NotificationLog(Long userId, String eventId, String message) {
        this.userId = userId;
        this.eventId = eventId;
        this.message = message;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = KstDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
