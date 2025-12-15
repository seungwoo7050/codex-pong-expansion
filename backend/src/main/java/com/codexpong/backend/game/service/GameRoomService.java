package com.codexpong.backend.game.service;

import com.codexpong.backend.game.GameResult;
import com.codexpong.backend.game.GameResultService;
import com.codexpong.backend.game.domain.GameRoom;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.game.engine.model.GameSnapshot;
import com.codexpong.backend.game.engine.model.PaddleInput;
import com.codexpong.backend.replay.ReplayService;
import com.codexpong.backend.user.domain.User;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/game/service/GameRoomService.java
 * 설명:
 *   - 경기 방 생성/관리와 틱 루프 실행, 상태 브로드캐스트를 담당한다.
 *   - 방이 종료되면 GameResultService를 통해 DB에 기록한다.
 *   - v0.8.0에서는 관전자 연결 제한과 지연 브로드캐스트를 포함한 관전 지원을 수행한다.
 *   - v0.11.0에서는 틱 단위 스냅샷을 리플레이 버퍼에 기록해 종료 시 파일을 생성한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/backend/v0.8.0-spectator-mode.md
 *   - design/realtime/v0.8.0-spectator-events.md
 *   - design/backend/v0.11.0-replay-recording-and-storage.md
 * 변경 이력:
 *   - v0.9.0: 활성 경기/관전자 계수 메트릭 노출 함수 추가
 */
@Service
public class GameRoomService {

    private static final Duration TICK_INTERVAL = Duration.ofMillis(50);
    private static final Duration SPECTATOR_DELAY = Duration.ofMillis(250);
    private static final int MAX_SPECTATORS_PER_ROOM = 30;

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> loopHandles = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, WebSocketSession>> spectatorSessions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final GameResultService gameResultService;
    private final ReplayService replayService;
    private final ObjectMapper objectMapper;

    public GameRoomService(GameResultService gameResultService, ReplayService replayService, ObjectMapper objectMapper) {
        this.gameResultService = gameResultService;
        this.replayService = replayService;
        this.objectMapper = objectMapper;
    }

    public GameRoom createRoom(User left, User right, MatchType matchType) {
        GameRoom room = new GameRoom(left, right, matchType);
        rooms.put(room.getRoomId(), room);
        replayService.startRecording(room);
        return room;
    }

    public Optional<GameRoom> findRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public void removeRoom(String roomId) {
        Optional.ofNullable(loopHandles.remove(roomId)).ifPresent(handle -> handle.cancel(true));
        rooms.remove(roomId);
        roomSessions.remove(roomId);
        spectatorSessions.remove(roomId);
    }

    public void updateInput(String roomId, Long userId, PaddleInput input) {
        GameRoom room = rooms.get(roomId);
        if (room != null && room.contains(userId)) {
            room.updateInput(userId, input);
        }
    }

    public void registerSession(GameRoom room, Long userId, WebSocketSession session) {
        roomSessions.computeIfAbsent(room.getRoomId(), key -> new ConcurrentHashMap<>())
                .put(userId, session);
        if (!loopHandles.containsKey(room.getRoomId()) && hasBothPlayers(room.getRoomId())) {
            startLoop(room);
        }
    }

    public boolean registerSpectatorSession(GameRoom room, String sessionId, WebSocketSession session) {
        Map<String, WebSocketSession> spectators = spectatorSessions
                .computeIfAbsent(room.getRoomId(), key -> new ConcurrentHashMap<>());
        if (spectators.size() >= MAX_SPECTATORS_PER_ROOM) {
            return false;
        }
        spectators.put(sessionId, session);
        return true;
    }

    public void unregisterSession(String roomId, Long userId, String sessionId) {
        Optional.ofNullable(roomSessions.get(roomId))
                .ifPresent(map -> map.entrySet().removeIf(entry -> (userId != null && entry.getKey().equals(userId))
                        || entry.getValue().getId().equals(sessionId)));
        Optional.ofNullable(spectatorSessions.get(roomId))
                .ifPresent(map -> map.entrySet().removeIf(entry -> entry.getKey().equals(sessionId)));
    }

    public List<LiveRoomView> listLiveRooms() {
        List<LiveRoomView> liveRooms = new ArrayList<>();
        for (GameRoom room : rooms.values()) {
            liveRooms.add(new LiveRoomView(
                    room.getRoomId(),
                    room.getLeftPlayer().getId(),
                    room.getLeftPlayer().getNickname(),
                    room.getRightPlayer().getId(),
                    room.getRightPlayer().getNickname(),
                    room.getMatchType(),
                    room.getStartedAt(),
                    room.getFinishedAt(),
                    spectatorCount(room.getRoomId()),
                    MAX_SPECTATORS_PER_ROOM
            ));
        }
        return Collections.unmodifiableList(liveRooms);
    }

    /**
     * 설명:
     *   - 모니터링을 위해 현재 메모리에 존재하는 활성 경기 방 수를 반환한다.
     */
    public int activeRoomCount() {
        return rooms.size();
    }

    /**
     * 설명:
     *   - 전체 관전자 세션 수를 합산해 관리자 통계에 제공한다.
     */
    public int totalSpectatorCount() {
        return spectatorSessions.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

    public int spectatorCount(String roomId) {
        return spectatorSessions.getOrDefault(roomId, Collections.emptyMap()).size();
    }

    private boolean hasBothPlayers(String roomId) {
        Map<Long, WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return false;
        }
        GameRoom room = rooms.get(roomId);
        return room != null
                && sessions.containsKey(room.getLeftPlayer().getId())
                && sessions.containsKey(room.getRightPlayer().getId());
    }

    private void startLoop(GameRoom room) {
        ScheduledFuture<?> handle = scheduler.scheduleAtFixedRate(() -> runTick(room), 0,
                TICK_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
        loopHandles.put(room.getRoomId(), handle);
    }

    private void runTick(GameRoom room) {
        GameSnapshot snapshot = room.tick(TICK_INTERVAL);
        replayService.appendSnapshot(room.getRoomId(), snapshot);
        broadcastState(room.getRoomId(), snapshot, room.getMatchType(), null);
        if (snapshot.finished()) {
            finishRoom(room, snapshot);
        }
    }

    private void broadcastState(String roomId, GameSnapshot snapshot, MatchType matchType,
            GameResult ratingResult) {
        GameServerMessage playerMessage = buildMessage("STATE", snapshot, matchType, ratingResult,
                AudienceRole.PLAYER, roomId);
        GameServerMessage spectatorMessage = buildMessage("STATE", snapshot, matchType, ratingResult,
                AudienceRole.SPECTATOR, roomId);
        sendMessage(roomSessions.get(roomId), playerMessage, 0);
        sendMessage(spectatorSessions.get(roomId), spectatorMessage, SPECTATOR_DELAY.toMillis());
    }

    private void finishRoom(GameRoom room, GameSnapshot snapshot) {
        GameResult result = gameResultService.recordResult(
                room.getRoomId(),
                room.getLeftPlayer(),
                room.getRightPlayer(),
                snapshot.leftScore(),
                snapshot.rightScore(),
                room.getMatchType(),
                room.getStartedAt(),
                room.getFinishedAt() != null ? room.getFinishedAt() : LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        );
        replayService.completeRecording(room, result);
        broadcastState(room.getRoomId(), snapshot, room.getMatchType(), result);
        removeRoom(room.getRoomId());
    }

    private GameServerMessage buildMessage(String type, GameSnapshot snapshot, MatchType matchType,
            GameResult ratingResult, AudienceRole audienceRole, String roomId) {
        return new GameServerMessage(type, snapshot, matchType.name(),
                ratingResult == null ? null : GameServerMessage.RatingChange.from(ratingResult),
                audienceRole.name(), spectatorCount(roomId));
    }

    private void sendMessage(Map<?, WebSocketSession> sessions, GameServerMessage message, long delayMillis) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(message);
            Runnable sender = () -> sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(payload));
                    }
                } catch (IOException ignored) {
                }
            });
            if (delayMillis <= 0) {
                sender.run();
            } else {
                scheduler.schedule(sender, delayMillis, TimeUnit.MILLISECONDS);
            }
        } catch (IOException ignored) {
        }
    }

    public enum AudienceRole {
        PLAYER,
        SPECTATOR
    }

    public record GameServerMessage(String type, GameSnapshot snapshot, String matchType, RatingChange ratingChange,
            String audienceRole, int spectatorCount) {

        public record RatingChange(Long winnerId, int winnerDelta, Long loserId, int loserDelta) {

            static RatingChange from(GameResult result) {
                if (!result.isRanked()) {
                    return null;
                }
                Long winnerId = result.getScoreA() == result.getScoreB()
                        ? null
                        : (result.getScoreA() > result.getScoreB() ? result.getPlayerA().getId()
                                : result.getPlayerB().getId());
                int deltaA = result.getRatingChangeA();
                int deltaB = result.getRatingChangeB();
                if (winnerId == null) {
                    return new RatingChange(null, deltaA, null, deltaB);
                }
                boolean playerAWin = winnerId.equals(result.getPlayerA().getId());
                return new RatingChange(winnerId,
                        playerAWin ? deltaA : deltaB,
                        playerAWin ? result.getPlayerB().getId() : result.getPlayerA().getId(),
                        playerAWin ? deltaB : deltaA);
            }
        }
    }

    public record LiveRoomView(String roomId, Long leftPlayerId, String leftNickname, Long rightPlayerId,
            String rightNickname, MatchType matchType, LocalDateTime startedAt, LocalDateTime finishedAt,
            int spectatorCount, int spectatorLimit) {
    }
}
