package com.codexpong.backend.game;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.game.domain.GameRoom;
import com.codexpong.backend.game.engine.model.PaddleInput;
import com.codexpong.backend.game.service.GameRoomService;
import com.codexpong.backend.game.service.GameRoomService.AudienceRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * [핸들러] backend/src/main/java/com/codexpong/backend/game/GameWebSocketHandler.java
 * 설명:
 *   - 빠른 대전으로 생성된 경기 방에 대한 WebSocket 연결을 관리한다.
 *   - 클라이언트 입력을 GameRoomService로 전달하고, 초기 상태를 전송한다.
 *   - v0.8.0에서는 관전 모드 진입을 허용하고 입력 차단, 관전자 수 제한을 적용한다.
 * 버전: v0.8.0
 * 관련 설계문서:
 *   - design/realtime/v0.8.0-spectator-events.md
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final GameRoomService gameRoomService;
    private final ObjectMapper objectMapper;

    public GameWebSocketHandler(GameRoomService gameRoomService, ObjectMapper objectMapper) {
        this.gameRoomService = gameRoomService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        AuthenticatedUser user = session.getPrincipal() instanceof AuthenticatedUser principal ? principal : null;
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("인증이 필요합니다."));
            return;
        }
        String roomId = extractRoomId(session.getUri());
        AudienceRole audienceRole = resolveAudienceRole(session.getUri());
        if (roomId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("roomId가 필요합니다."));
            return;
        }
        Optional<GameRoom> roomOpt = gameRoomService.findRoom(roomId);
        if (roomOpt.isEmpty()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("참가할 수 없는 방입니다."));
            return;
        }
        GameRoom room = roomOpt.get();
        if (audienceRole == AudienceRole.PLAYER && !room.contains(user.id())) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("참가할 수 없는 방입니다."));
            return;
        }
        if (room.isFinished()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("종료된 방은 관전할 수 없습니다."));
            return;
        }

        if (audienceRole == AudienceRole.SPECTATOR) {
            if (!gameRoomService.registerSpectatorSession(room, session.getId(), session)) {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("관전자 수가 가득 찼습니다."));
                return;
            }
        } else {
            gameRoomService.registerSession(room, user.id(), session);
        }
        session.getAttributes().put("audienceRole", audienceRole.name());
        sendServerMessage(session, new GameRoomService.GameServerMessage("READY", room.currentSnapshot(),
                room.getMatchType().name(), null, audienceRole.name(), gameRoomService.spectatorCount(roomId)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        AuthenticatedUser user = session.getPrincipal() instanceof AuthenticatedUser principal ? principal : null;
        if (user == null) {
            return;
        }
        AudienceRole role = AudienceRole.valueOf(String.valueOf(session.getAttributes().getOrDefault("audienceRole",
                AudienceRole.PLAYER.name())));
        if (role == AudienceRole.SPECTATOR) {
            return;
        }
        ClientMessage clientMessage = objectMapper.readValue(message.getPayload(), ClientMessage.class);
        if (clientMessage.type().equals("INPUT")) {
            PaddleInput input = parseInput(clientMessage.direction());
            if (input != null && clientMessage.roomId() != null) {
                gameRoomService.updateInput(clientMessage.roomId(), user.id(), input);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = extractRoomId(session.getUri());
        AuthenticatedUser user = session.getPrincipal() instanceof AuthenticatedUser principal ? principal : null;
        gameRoomService.unregisterSession(roomId, user != null ? user.id() : null, session.getId());
        super.afterConnectionClosed(session, status);
    }

    private PaddleInput parseInput(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.toUpperCase()) {
            case "UP" -> PaddleInput.UP;
            case "DOWN" -> PaddleInput.DOWN;
            default -> PaddleInput.STAY;
        };
    }

    private void sendServerMessage(WebSocketSession session, GameRoomService.GameServerMessage message) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException ignored) {
        }
    }

    private String extractRoomId(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        Map<String, String> params = QueryStringUtils.parse(uri.getQuery());
        return params.get("roomId");
    }

    private AudienceRole resolveAudienceRole(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return AudienceRole.PLAYER;
        }
        Map<String, String> params = QueryStringUtils.parse(uri.getQuery());
        String role = params.get("role");
        if (role != null && role.equalsIgnoreCase("spectator")) {
            return AudienceRole.SPECTATOR;
        }
        return AudienceRole.PLAYER;
    }

    public record ClientMessage(String type, String roomId, String direction) {
    }

    private static class QueryStringUtils {
        static Map<String, String> parse(String query) {
            String[] pairs = query.split("&");
            Map<String, String> values = new java.util.HashMap<>();
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    values.put(keyValue[0], URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                }
            }
            return values;
        }
    }
}
