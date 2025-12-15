package com.codexpong.backend.game;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * [핸들러] backend/src/main/java/com/codexpong/backend/game/EchoWebSocketHandler.java
 * 설명:
 *   - 프런트엔드가 WebSocket 연결을 확인할 수 있도록 단순 에코 메시지를 반환한다.
 *   - 인증된 사용자 정보를 세션에서 확인하고, 추후 게임 이벤트 연동 시 연결 주체를 구분할 수 있는 구조를 준비한다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/realtime/v0.1.0-basic-websocket-wiring.md
 *   - design/backend/v0.2.0-auth-and-profile.md
 * 변경 이력:
 *   - v0.1.0: 에코 핸들러 최초 구현
 *   - v0.2.0: 인증 사용자 닉네임을 포함한 에코 응답으로 확장
 */
@Component
public class EchoWebSocketHandler extends TextWebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        AuthenticatedUser user = session.getPrincipal() instanceof AuthenticatedUser principal ? principal : null;
        String sender = user != null ? user.nickname() : "anonymous";
        session.sendMessage(new TextMessage(sender + "::" + message.getPayload()));
    }
}
