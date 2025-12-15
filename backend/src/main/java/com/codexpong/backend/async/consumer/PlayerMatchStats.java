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
 * [엔티티] backend/src/main/java/com/codexpong/backend/async/consumer/PlayerMatchStats.java
 * 설명:
 *   - 사용자별 승/패/무 및 총 경기 수를 집계하는 통계 프로젝션이다.
 */
@Entity
@Table(name = "player_match_stats")
public class PlayerMatchStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @Column(nullable = false)
    private int draws;

    @Column(nullable = false)
    private int totalMatches;

    @Column(nullable = false, length = 40)
    private String lastEventId;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected PlayerMatchStats() {
    }

    public PlayerMatchStats(Long userId, String lastEventId) {
        this.userId = userId;
        this.lastEventId = lastEventId;
    }

    public void recordWin(String lastEventId) {
        this.wins += 1;
        this.totalMatches += 1;
        this.lastEventId = lastEventId;
    }

    public void recordLoss(String lastEventId) {
        this.losses += 1;
        this.totalMatches += 1;
        this.lastEventId = lastEventId;
    }

    public void recordDraw(String lastEventId) {
        this.draws += 1;
        this.totalMatches += 1;
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

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getDraws() {
        return draws;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public String getLastEventId() {
        return lastEventId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
