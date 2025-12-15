package com.codexpong.backend.chat.service;

import com.codexpong.backend.chat.dto.ChatMessageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * [퍼블리셔] backend/src/main/java/com/codexpong/backend/chat/service/ChatEventPublisher.java
 * 설명:
 *   - 사용자별, 채널별 WebSocket 세션을 등록하고 메시지를 브로드캐스트한다.
 *   - 로비와 매치 방 채널은 channelKey를 통해 구독을 관리한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/realtime/v0.6.0-chat-events.md
 */
@Component
public class ChatEventPublisher {

    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> channelSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionChannels = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public ChatEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registerUserSession(Long userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void joinChannel(String channelKey, WebSocketSession session) {
        channelSessions.computeIfAbsent(channelKey, key -> ConcurrentHashMap.newKeySet()).add(session);
        sessionChannels.computeIfAbsent(session.getId(), key -> ConcurrentHashMap.newKeySet()).add(channelKey);
    }

    public void removeSession(WebSocketSession session) {
        userSessions.values().forEach(set -> set.remove(session));
        Set<String> channels = sessionChannels.getOrDefault(session.getId(), Collections.emptySet());
        channels.forEach(channel -> channelSessions.getOrDefault(channel, Collections.emptySet()).remove(session));
        sessionChannels.remove(session.getId());
    }

    public void publishToUser(Long userId, ChatMessageResponse message) {
        Set<WebSocketSession> sessions = userSessions.getOrDefault(userId, Collections.emptySet());
        sendToSessions(sessions, message);
    }

    public void publishToChannel(String channelKey, ChatMessageResponse message) {
        Set<WebSocketSession> sessions = channelSessions.getOrDefault(channelKey, Collections.emptySet());
        sendToSessions(sessions, message);
    }

    private void sendToSessions(Set<WebSocketSession> sessions, ChatMessageResponse message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            sessions.forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(payload));
                    } catch (IOException ignored) {
                    }
                }
            });
        } catch (IOException ignored) {
        }
    }
}
