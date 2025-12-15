package com.codexpong.backend.async.outbox;

import com.codexpong.backend.common.KstDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/async/outbox/OutboxEvent.java
 * 설명:
 *   - 도메인 트랜잭션과 함께 기록되는 아웃박스 이벤트를 보관한다.
 *   - payload는 JSON 문자열로 직렬화해 재시도와 DLQ 대상에도 동일하게 저장한다.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String eventId;

    @Column(nullable = false, length = 120)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 500)
    private String lastError;

    protected OutboxEvent() {
    }

    public OutboxEvent(String type, String payload) {
        this.eventId = UUID.randomUUID().toString();
        this.type = type;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = KstDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = KstDateTime.now();
    }

    public void markAttempt(String errorMessage) {
        this.attempts += 1;
        this.lastError = errorMessage;
        this.status = OutboxStatus.PENDING;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.lastError = null;
    }

    public void markFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.lastError = errorMessage;
    }

    public void resetForRetry() {
        this.status = OutboxStatus.PENDING;
        this.lastError = null;
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

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void overridePayload(String payload) {
        this.payload = payload;
    }
}
