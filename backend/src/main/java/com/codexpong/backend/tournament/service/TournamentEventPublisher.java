package com.codexpong.backend.tournament.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * [이벤트 퍼블리셔] backend/src/main/java/com/codexpong/backend/tournament/service/TournamentEventPublisher.java
 * 설명:
 *   - 토너먼트 관련 WebSocket 알림을 참가자에게 전송한다.
 *   - 사용자별 세션을 유지하면서 READY/완료 알림을 전달한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/realtime/v0.7.0-tournament-events.md
 */
@Component
public class TournamentEventPublisher {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public TournamentEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remove(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(session);
        if (userSessions.isEmpty()) {
            sessions.remove(userId);
        }
    }

    public void publishToUsers(Collection<Long> userIds, String type, Object payload) {
        for (Long userId : userIds) {
            send(userId, type, payload);
        }
    }

    public void publishSingle(Long userId, String type, Object payload) {
        send(userId, type, payload);
    }

    private void send(Long userId, String type, Object payload) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "payload", payload
            ));
            TextMessage textMessage = new TextMessage(message);
            userSessions.forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
