package com.codexpong.backend.config;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * [핸드셰이크 핸들러] backend/src/main/java/com/codexpong/backend/config/WebSocketUserHandshakeHandler.java
 * 설명:
 *   - WebSocketAuthHandshakeInterceptor에서 저장한 인증 정보를 Principal로 연결한다.
 *   - 인증 정보가 없을 경우 null을 반환하여 연결을 거부한다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/realtime/v0.1.0-basic-websocket-wiring.md
 *   - design/backend/v0.2.0-auth-and-profile.md
 * 변경 이력:
 *   - v0.2.0: 인증 사용자 정보를 WebSocket Principal로 설정
 */
public class WebSocketUserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        Object principal = attributes.get(WebSocketAuthHandshakeInterceptor.AUTH_USER_KEY);
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }
        return null;
    }
}
