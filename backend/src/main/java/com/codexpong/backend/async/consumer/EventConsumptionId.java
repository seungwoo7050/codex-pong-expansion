package com.codexpong.backend.async.consumer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * [식별자] backend/src/main/java/com/codexpong/backend/async/consumer/EventConsumptionId.java
 * 설명:
 *   - 소비자별 멱등성 기록을 위한 복합 키다.
 */
@Embeddable
public class EventConsumptionId implements Serializable {

    @Column(nullable = false, length = 40)
    private String eventId;

    @Column(nullable = false, length = 60)
    private String consumerName;

    protected EventConsumptionId() {
    }

    public EventConsumptionId(String eventId, String consumerName) {
        this.eventId = eventId;
        this.consumerName = consumerName;
    }

    public String getEventId() {
        return eventId;
    }

    public String getConsumerName() {
        return consumerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventConsumptionId that)) return false;
        return Objects.equals(eventId, that.eventId) && Objects.equals(consumerName, that.consumerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, consumerName);
    }
}
