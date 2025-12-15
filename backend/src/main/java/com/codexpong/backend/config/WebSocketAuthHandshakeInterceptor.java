package com.codexpong.backend.config;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.auth.service.AuthService;
import com.codexpong.backend.auth.service.AuthTokenService;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * [인터셉터] backend/src/main/java/com/codexpong/backend/config/WebSocketAuthHandshakeInterceptor.java
 * 설명:
 *   - WebSocket 핸드셰이크 시 전달된 JWT를 검증하고, 연결된 사용자를 속성에 기록한다.
 *   - `token` 쿼리 파라미터만 허용해 인증 경로를 단일화한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.2.0-auth-and-profile.md
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 * 변경 이력:
 *   - v0.2.0: JWT 검증 기반 핸드셰이크 인터셉터 추가
 *   - v0.10.0: WebSocket 인증 경로를 쿼리 파라미터로 단일화
 */
@Component
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    public static final String AUTH_USER_KEY = "authenticatedUser";

    private final AuthTokenService authTokenService;
    private final AuthService authService;

    public WebSocketAuthHandshakeInterceptor(AuthTokenService authTokenService, AuthService authService) {
        this.authTokenService = authTokenService;
        this.authService = authService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null) {
            return false;
        }
        Optional<AuthenticatedUser> authenticatedUser = authTokenService.parse(token)
                .flatMap(user -> authService.findById(user.id()).map(authService::toAuthenticatedUser));
        authenticatedUser.ifPresent(user -> attributes.put(AUTH_USER_KEY, user));
        return authenticatedUser.isPresent();
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Exception exception) {
        // 연결 완료 후 별도 작업 없음
    }

    private String extractToken(ServerHttpRequest request) {
        URI uri = request.getURI();
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        return params.getFirst("token");
    }
}
