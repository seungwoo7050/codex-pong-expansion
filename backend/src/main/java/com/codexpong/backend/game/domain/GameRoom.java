package com.codexpong.backend.game.domain;

import com.codexpong.backend.game.engine.GameEngine;
import com.codexpong.backend.game.engine.model.GameSnapshot;
import com.codexpong.backend.game.engine.model.PaddleInput;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.user.domain.User;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [도메인] backend/src/main/java/com/codexpong/backend/game/domain/GameRoom.java
 * 설명:
 *   - 두 명의 사용자가 참여하는 실시간 경기 방 상태를 보관한다.
 *   - 입력 큐와 게임 엔진을 연결해 스냅샷을 제공하고 종료 시간을 기록한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 */
public class GameRoom {

    private final String roomId;
    private final User leftPlayer;
    private final User rightPlayer;
    private final MatchType matchType;
    private final GameEngine engine;
    private final Map<Long, PaddleInput> inputs = new ConcurrentHashMap<>();

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public GameRoom(User leftPlayer, User rightPlayer, MatchType matchType) {
        this.leftPlayer = leftPlayer;
        this.rightPlayer = rightPlayer;
        this.matchType = matchType;
        this.engine = new GameEngine();
        this.roomId = Objects.requireNonNullElse(engine.forceSnapshot().roomId(), UUID.randomUUID().toString());
        this.inputs.put(leftPlayer.getId(), PaddleInput.STAY);
        this.inputs.put(rightPlayer.getId(), PaddleInput.STAY);
    }

    public boolean contains(Long userId) {
        return leftPlayer.getId().equals(userId) || rightPlayer.getId().equals(userId);
    }

    public void updateInput(Long userId, PaddleInput input) {
        inputs.put(userId, input);
    }

    public GameSnapshot tick(Duration delta) {
        if (startedAt == null) {
            startedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        }
        GameSnapshot snapshot = engine.tick(delta,
                inputs.getOrDefault(leftPlayer.getId(), PaddleInput.STAY),
                inputs.getOrDefault(rightPlayer.getId(), PaddleInput.STAY));
        if (snapshot.finished() && finishedAt == null) {
            finishedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        }
        return snapshot;
    }

    public GameSnapshot currentSnapshot() {
        return engine.forceSnapshot();
    }

    public String getRoomId() {
        return roomId;
    }

    public User getLeftPlayer() {
        return leftPlayer;
    }

    public User getRightPlayer() {
        return rightPlayer;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public boolean isFinished() {
        GameSnapshot snapshot = engine.forceSnapshot();
        return snapshot.finished();
    }

    public int getTargetScore() {
        return engine.getTargetScore();
    }
}
