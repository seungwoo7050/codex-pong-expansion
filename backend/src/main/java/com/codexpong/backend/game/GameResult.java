package com.codexpong.backend.game;

import com.codexpong.backend.game.domain.MatchType;
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
 * [엔티티] backend/src/main/java/com/codexpong/backend/game/GameResult.java
 * 설명:
 *   - v0.3.0 실시간 경기 종료 후 결과를 영속화하기 위한 엔티티다.
 *   - 사용자 엔티티와 연결하여 추후 전적/랭킹으로 확장 가능한 형태를 유지한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 * 변경 이력:
 *   - v0.1.0: 기본 필드 정의 및 자동 증가 ID 추가
 *   - v0.3.0: User 연관 관계와 룸/시간 정보를 포함한 전적 구조로 확장
 *   - v0.4.0: 랭크전 여부와 레이팅 변동 기록을 추가
 */
@Entity
@Table(name = "game_results")
public class GameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_a_id")
    private User playerA;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_b_id")
    private User playerB;

    @Column(nullable = false)
    private int scoreA;

    @Column(nullable = false)
    private int scoreB;

    @Column(nullable = false, length = 100)
    private String roomId;

    @Column(nullable = false, length = 20)
    private String matchType;

    @Column(nullable = false)
    private int ratingChangeA;

    @Column(nullable = false)
    private int ratingChangeB;

    @Column(nullable = false)
    private int ratingAfterA;

    @Column(nullable = false)
    private int ratingAfterB;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime finishedAt;

    protected GameResult() {
    }

    public GameResult(User playerA, User playerB, int scoreA, int scoreB, String roomId, MatchType matchType,
            int ratingChangeA, int ratingChangeB, int ratingAfterA, int ratingAfterB, LocalDateTime startedAt,
            LocalDateTime finishedAt) {
        this.playerA = playerA;
        this.playerB = playerB;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.roomId = roomId;
        this.matchType = matchType.name();
        this.ratingChangeA = ratingChangeA;
        this.ratingChangeB = ratingChangeB;
        this.ratingAfterA = ratingAfterA;
        this.ratingAfterB = ratingAfterB;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public Long getId() {
        return id;
    }

    public User getPlayerA() {
        return playerA;
    }

    public User getPlayerB() {
        return playerB;
    }

    public int getScoreA() {
        return scoreA;
    }

    public int getScoreB() {
        return scoreB;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getMatchType() {
        return matchType;
    }

    public boolean isRanked() {
        return MatchType.RANKED.name().equals(matchType);
    }

    public int getRatingChangeA() {
        return ratingChangeA;
    }

    public int getRatingChangeB() {
        return ratingChangeB;
    }

    public int getRatingAfterA() {
        return ratingAfterA;
    }

    public int getRatingAfterB() {
        return ratingAfterB;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }
}
