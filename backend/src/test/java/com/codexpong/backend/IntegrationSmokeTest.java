package com.codexpong.backend;

import com.codexpong.backend.auth.dto.AuthResponse;
import com.codexpong.backend.auth.dto.LoginRequest;
import com.codexpong.backend.auth.dto.RegisterRequest;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/IntegrationSmokeTest.java
 * 설명:
 *   - JWT 로그인 후 보호된 REST API와 WebSocket 연결이 동작하는지 최소 흐름을 검증한다.
 *   - OpenAPI 문서 엔드포인트가 활성화되어 있는지도 함께 확인한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 * 변경 이력:
 *   - v0.10.0: REST/웹소켓 스모크 테스트 및 OpenAPI 헬스체크 추가
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void 로그인_후_보호_API를_호출할_수_있다() {
        AuthResponse authResponse = registerAndLogin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authResponse.getToken());

        ResponseEntity<Map> meResponse = restTemplate.exchange(
                baseUrl("/api/users/me"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        Assertions.assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(meResponse.getBody()).isNotNull();
        Assertions.assertThat(meResponse.getBody().get("username"))
                .isEqualTo(authResponse.getUser().getUsername());
    }

    @Test
    void 에코_WebSocket이_JWT_기반_연결로_메시지를_왕복한다() throws Exception {
        AuthResponse authResponse = registerAndLogin();
        String token = authResponse.getToken();
        String payload = "ping-" + UUID.randomUUID();

        StandardWebSocketClient client = new StandardWebSocketClient();
        CompletableFuture<String> received = new CompletableFuture<>();
        WebSocketSession session = client.doHandshake(new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                session.sendMessage(new TextMessage(payload));
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                received.complete(message.getPayload());
            }
        }, null, URI.create(String.format("ws://localhost:%d/ws/echo?token=%s", port, token))).get(5, TimeUnit.SECONDS);

        try {
            String echoed = received.get(5, TimeUnit.SECONDS);
            Assertions.assertThat(echoed).contains(payload);
        } finally {
            session.close();
        }
    }

    @Test
    void 토큰_없이_WebSocket_연결을_거부한다() {
        StandardWebSocketClient client = new StandardWebSocketClient();

        Assertions.assertThatThrownBy(() -> client.doHandshake(new TextWebSocketHandler() { },
                        null, URI.create(String.format("ws://localhost:%d/ws/echo", port)))
                .get(5, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
    }

    @Test
    void 스웨거_문서_JSON_엔드포인트가_응답한다() {
        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl("/v3/api-docs"), Map.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody()).containsKey("openapi");
    }

    private AuthResponse registerAndLogin() {
        String username = "smoke" + UUID.randomUUID();
        RegisterRequest registerRequest = new RegisterRequest(username, "password123", "스모크", null);
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                baseUrl("/api/auth/register"), registerRequest, AuthResponse.class);
        Assertions.assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        LoginRequest loginRequest = new LoginRequest(username, "password123");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                baseUrl("/api/auth/login"), loginRequest, AuthResponse.class);
        Assertions.assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(loginResponse.getBody()).isNotNull();
        return loginResponse.getBody();
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
