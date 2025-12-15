package com.codexpong.backend.chat;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.chat.dto.ChatMessageResponse;
import com.codexpong.backend.chat.dto.ChatSocketMessage;
import com.codexpong.backend.chat.service.ChatEventPublisher;
import com.codexpong.backend.chat.service.ChatService;
import com.codexpong.backend.config.WebSocketAuthHandshakeInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * [핸들러] backend/src/main/java/com/codexpong/backend/chat/ChatWebSocketHandler.java
 * 설명:
 *   - 채팅 WebSocket 연결을 관리하고 DM/로비/매치 채팅 명령을 처리한다.
 *   - 연결 즉시 로비 채널을 구독하며, 별도 명령으로 매치 채널 구독을 추가한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/realtime/v0.6.0-chat-events.md
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String LOBBY_KEY = "GLOBAL_LOBBY";

    private final ChatService chatService;
    private final ChatEventPublisher chatEventPublisher;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(ChatService chatService, ChatEventPublisher chatEventPublisher, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.chatEventPublisher = chatEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        AuthenticatedUser user = (AuthenticatedUser) session.getAttributes()
                .getOrDefault(WebSocketAuthHandshakeInterceptor.AUTH_USER_KEY, null);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("인증 정보가 필요합니다."));
            return;
        }
        chatEventPublisher.registerUserSession(user.id(), session);
        chatEventPublisher.joinChannel(LOBBY_KEY, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        AuthenticatedUser user = (AuthenticatedUser) session.getAttributes()
                .getOrDefault(WebSocketAuthHandshakeInterceptor.AUTH_USER_KEY, null);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("인증 정보가 필요합니다."));
            return;
        }
        ChatSocketMessage command = objectMapper.readValue(message.getPayload(), ChatSocketMessage.class);
        if ("SUBSCRIBE_MATCH".equals(command.getType())) {
            if (command.getRoomId() != null && !command.getRoomId().isBlank()) {
                chatEventPublisher.joinChannel(command.getRoomId(), session);
            }
            return;
        }
        ChatMessageResponse response = chatService.handleSocketCommand(user.id(), command);
        switch (response.getChannelType()) {
            case "DM" -> {
                chatEventPublisher.publishToUser(user.id(), response);
                if (response.getRecipientId() != null) {
                    chatEventPublisher.publishToUser(response.getRecipientId(), response);
                }
            }
            case "LOBBY" -> chatEventPublisher.publishToChannel(LOBBY_KEY, response);
            case "MATCH" -> chatEventPublisher.publishToChannel(response.getChannelKey(), response);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "알 수 없는 채널 유형입니다.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        chatEventPublisher.removeSession(session);
    }
}
