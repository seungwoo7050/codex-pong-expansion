package com.codexpong.backend.social;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.config.WebSocketAuthHandshakeInterceptor;
import com.codexpong.backend.social.service.SocialEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * [핸들러] backend/src/main/java/com/codexpong/backend/social/SocialWebSocketHandler.java
 * 설명:
 *   - 친구/초대 알림을 받을 WebSocket 연결을 등록하고 유지한다.
 *   - 메시지 수신 시 간단한 pong 응답으로 연결 상태를 확인한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
@Component
public class SocialWebSocketHandler extends TextWebSocketHandler {

    private final SocialEventPublisher socialEventPublisher;

    public SocialWebSocketHandler(SocialEventPublisher socialEventPublisher) {
        this.socialEventPublisher = socialEventPublisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        AuthenticatedUser user = (AuthenticatedUser) session.getAttributes()
                .getOrDefault(WebSocketAuthHandshakeInterceptor.AUTH_USER_KEY, null);
        if (user != null) {
            socialEventPublisher.registerSession(user.id(), session);
        } else {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("인증 정보가 필요합니다."));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        AuthenticatedUser user = (AuthenticatedUser) session.getAttributes()
                .getOrDefault(WebSocketAuthHandshakeInterceptor.AUTH_USER_KEY, null);
        if (user != null) {
            socialEventPublisher.removeSession(user.id(), session);
        }
    }
}
