package com.codexpong.backend.tournament.domain;

import com.codexpong.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/tournament/domain/Tournament.java
 * 설명:
 *   - 단일 제거 토너먼트의 기본 정보를 보관하며 참가/시작/완료 상태를 추적한다.
 *   - 생성자와 상태 전환 시각을 기록해 히스토리성 조회에 활용한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TournamentStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    protected Tournament() {
    }

    public Tournament(String name, Integer maxParticipants, User creator) {
        this.name = name;
        this.maxParticipants = maxParticipants;
        this.creator = creator;
        this.status = TournamentStatus.REGISTRATION;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void markInProgress() {
        this.status = TournamentStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = TournamentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public User getCreator() {
        return creator;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
