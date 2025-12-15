package com.codexpong.backend.tournament;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.config.WebSocketAuthHandshakeInterceptor;
import com.codexpong.backend.tournament.service.TournamentEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * [핸들러] backend/src/main/java/com/codexpong/backend/tournament/TournamentWebSocketHandler.java
 * 설명:
 *   - 토너먼트 알림 수신을 위한 WebSocket 연결을 관리한다.
 *   - 토큰 기반 인증 정보를 세션에서 꺼내 참가자별 알림 채널을 만든다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/realtime/v0.7.0-tournament-events.md
 */
@Component
public class TournamentWebSocketHandler extends TextWebSocketHandler {

    private final TournamentEventPublisher tournamentEventPublisher;

    public TournamentWebSocketHandler(TournamentEventPublisher tournamentEventPublisher) {
        this.tournamentEventPublisher = tournamentEventPublisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        AuthenticatedUser user = (AuthenticatedUser) session.getAttributes()
                .getOrDefault(WebSocketAuthHandshakeInterceptor.AUTH_USER_KEY, null);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("인증이 필요합니다."));
            return;
        }
        tournamentEventPublisher.register(user.id(), session);
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
            tournamentEventPublisher.remove(user.id(), session);
        }
    }
}
