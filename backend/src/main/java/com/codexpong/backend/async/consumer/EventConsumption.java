package com.codexpong.backend.async.consumer;

import com.codexpong.backend.common.KstDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/async/consumer/EventConsumption.java
 * 설명:
 *   - 특정 소비자가 이벤트를 처리했음을 기록해 멱등성을 보장한다.
 */
@Entity
@Table(name = "event_consumptions")
public class EventConsumption {

    @EmbeddedId
    private EventConsumptionId id;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    protected EventConsumption() {
    }

    public EventConsumption(String eventId, String consumerName) {
        this.id = new EventConsumptionId(eventId, consumerName);
    }

    @PrePersist
    void onCreate() {
        this.processedAt = KstDateTime.now();
    }

    public EventConsumptionId getId() {
        return id;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
