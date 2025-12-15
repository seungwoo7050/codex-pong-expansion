package com.codexpong.backend.tournament.domain;

import com.codexpong.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/tournament/domain/TournamentParticipant.java
 * 설명:
 *   - 특정 토너먼트에 등록된 참가자 정보를 나타내며 사용자와 연결된다.
 *   - 시드 순서를 보관해 브래킷 생성 시 조합 순서를 결정한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
@Entity
@Table(name = "tournament_participants")
public class TournamentParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer seed;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    protected TournamentParticipant() {
    }

    public TournamentParticipant(Tournament tournament, User user, Integer seed) {
        this.tournament = tournament;
        this.user = user;
        this.seed = seed;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public User getUser() {
        return user;
    }

    public Integer getSeed() {
        return seed;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
