package com.codexpong.backend.social.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * [이벤트 퍼블리셔] backend/src/main/java/com/codexpong/backend/social/service/SocialEventPublisher.java
 * 설명:
 *   - 친구/초대 이벤트를 WebSocket 구독자에게 전달할 수 있는 최소한의 브로드캐스터 역할을 한다.
 *   - 사용자별 연결 세션을 추적해 온라인 상태 확인에도 활용한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
@Component
public class SocialEventPublisher {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SocialEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registerSession(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void removeSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(session);
        if (userSessions.isEmpty()) {
            sessions.remove(userId);
        }
    }

    public boolean isOnline(Long userId) {
        return sessions.containsKey(userId) && !sessions.get(userId).isEmpty();
    }

    public void publish(Long userId, String type, Object payload) {
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
            userSessions.forEach(session -> sendSafely(session, textMessage));
        } catch (IOException ignored) {
        }
    }

    private void sendSafely(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException ignored) {
        }
    }
}
