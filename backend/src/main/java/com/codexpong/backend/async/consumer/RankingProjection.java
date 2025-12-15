package com.codexpong.backend.async.consumer;

import com.codexpong.backend.common.KstDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/async/consumer/RankingProjection.java
 * 설명:
 *   - 랭킹 소비자가 최신 레이팅 스냅샷을 저장하는 프로젝션 테이블이다.
 */
@Entity
@Table(name = "ranking_projections")
public class RankingProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 40)
    private String lastEventId;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected RankingProjection() {
    }

    public RankingProjection(Long userId, int rating, String lastEventId) {
        this.userId = userId;
        this.rating = rating;
        this.lastEventId = lastEventId;
    }

    public void refresh(int rating, String lastEventId) {
        this.rating = rating;
        this.lastEventId = lastEventId;
    }

    @PrePersist
    void onCreate() {
        this.updatedAt = KstDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = KstDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getRating() {
        return rating;
    }

    public String getLastEventId() {
        return lastEventId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
