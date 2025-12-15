package com.codexpong.backend.async.consumer;

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
 * [엔티티] backend/src/main/java/com/codexpong/backend/async/consumer/AbuseSignal.java
 * 설명:
 *   - 경기 데이터 기반으로 탐지된 이상 징후를 보관한다.
 */
@Entity
@Table(name = "abuse_signals")
public class AbuseSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String roomId;

    @Column(nullable = false)
    private Long suspiciousUserId;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    protected AbuseSignal() {
    }

    public AbuseSignal(String eventId, String roomId, Long suspiciousUserId, String reason) {
        this.eventId = eventId;
        this.roomId = roomId;
        this.suspiciousUserId = suspiciousUserId;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        this.detectedAt = KstDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getRoomId() {
        return roomId;
    }

    public Long getSuspiciousUserId() {
        return suspiciousUserId;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }
}
