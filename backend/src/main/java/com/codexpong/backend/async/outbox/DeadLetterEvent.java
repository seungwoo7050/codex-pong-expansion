package com.codexpong.backend.async.outbox;

import com.codexpong.backend.common.KstDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/async/outbox/DeadLetterEvent.java
 * 설명:
 *   - 재시도 한계를 초과한 이벤트를 DLQ 용도로 보관한다.
 *   - 운영자가 재처리하거나 폐기할 수 있도록 원본 payload와 실패 메시지를 남긴다.
 */
@Entity
@Table(name = "dead_letter_events")
public class DeadLetterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String eventId;

    @Column(nullable = false, length = 120)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private int failedAttempts;

    @Column(length = 500)
    private String lastError;

    @Column(nullable = false)
    private LocalDateTime failedAt;

    protected DeadLetterEvent() {
    }

    public DeadLetterEvent(String eventId, String type, String payload, int failedAttempts, String lastError) {
        this.eventId = eventId;
        this.type = type;
        this.payload = payload;
        this.failedAttempts = failedAttempts;
        this.lastError = lastError;
    }

    @PrePersist
    void onCreate() {
        this.failedAt = KstDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }
}
