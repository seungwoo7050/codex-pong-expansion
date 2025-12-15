package com.codexpong.backend.tournament.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/tournament/domain/TournamentMatch.java
 * 설명:
 *   - 단일 토너먼트 내 개별 경기 매칭 정보를 표현한다.
 *   - 참가자, 라운드/포지션, roomId를 저장해 게임 서비스와 연결한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
@Entity
@Table(name = "tournament_matches")
public class TournamentMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "participant_a_id")
    private TournamentParticipant participantA;

    @ManyToOne
    @JoinColumn(name = "participant_b_id")
    private TournamentParticipant participantB;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private TournamentParticipant winner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TournamentMatchStatus status;

    @Column(nullable = false)
    private Integer roundNumber;

    @Column(nullable = false)
    private Integer position;

    @Column(length = 100)
    private String roomId;

    @Column
    private Integer scoreA;

    @Column
    private Integer scoreB;

    @Column
    private LocalDateTime completedAt;

    protected TournamentMatch() {
    }

    public TournamentMatch(Tournament tournament, Integer roundNumber, Integer position) {
        this.tournament = tournament;
        this.roundNumber = roundNumber;
        this.position = position;
        this.status = TournamentMatchStatus.PENDING;
    }

    public boolean readyToStart() {
        return participantA != null && participantB != null && status == TournamentMatchStatus.PENDING;
    }

    public void assignParticipants(TournamentParticipant first, TournamentParticipant second) {
        this.participantA = first;
        this.participantB = second;
    }

    public void markReady(String roomId) {
        this.status = TournamentMatchStatus.READY;
        this.roomId = roomId;
    }

    public void complete(Integer scoreA, Integer scoreB, TournamentParticipant winner) {
        this.status = TournamentMatchStatus.COMPLETED;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.winner = winner;
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public TournamentParticipant getParticipantA() {
        return participantA;
    }

    public TournamentParticipant getParticipantB() {
        return participantB;
    }

    public TournamentParticipant getWinner() {
        return winner;
    }

    public TournamentMatchStatus getStatus() {
        return status;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public Integer getPosition() {
        return position;
    }

    public String getRoomId() {
        return roomId;
    }

    public Integer getScoreA() {
        return scoreA;
    }

    public Integer getScoreB() {
        return scoreB;
    }
}
