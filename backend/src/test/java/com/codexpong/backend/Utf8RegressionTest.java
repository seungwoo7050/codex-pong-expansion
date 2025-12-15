package com.codexpong.backend;

import com.codexpong.backend.auth.dto.AuthResponse;
import com.codexpong.backend.auth.dto.LoginRequest;
import com.codexpong.backend.auth.dto.RegisterRequest;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.dto.ProfileUpdateRequest;
import com.codexpong.backend.user.dto.UserResponse;
import com.codexpong.backend.user.repository.UserRepository;
import java.net.URI;
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
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/Utf8RegressionTest.java
 * 설명:
 *   - v0.14.0에서 요구하는 utf8mb4(한글+이모지) 엔드 투 엔드 회귀를 보장한다.
 *   - REST 페이로드, WebSocket 메시지, DB 저장/조회가 모두 손상 없이 동작하는지 검증한다.
 * 버전: v0.14.0
 * 관련 설계문서:
 *   - design/backend/v0.14.0-utf8-regression-suite.md
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Utf8RegressionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 한글_이모지_닉네임이_rest_ws_db_전반에서_보존된다() throws Exception {
        String baseNickname = "코딩😀선수";
        AuthResponse authResponse = registerAndLogin(baseNickname);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authResponse.getToken());

        ResponseEntity<UserResponse> meResponse = restTemplate.exchange(
                baseUrl("/api/users/me"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponse.class
        );
        Assertions.assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(meResponse.getBody()).isNotNull();
        Assertions.assertThat(meResponse.getBody().getNickname()).isEqualTo(baseNickname);

        User saved = userRepository.findById(authResponse.getUser().getId()).orElseThrow();
        Assertions.assertThat(saved.getNickname()).isEqualTo(baseNickname);

        String updatedNickname = "페이로드🚀테스트";
        ResponseEntity<UserResponse> updateResponse = restTemplate.exchange(
                baseUrl("/api/users/me"),
                HttpMethod.PUT,
                new HttpEntity<>(new ProfileUpdateRequest(updatedNickname, null), headers),
                UserResponse.class
        );
        Assertions.assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(updateResponse.getBody()).isNotNull();
        Assertions.assertThat(updateResponse.getBody().getNickname()).isEqualTo(updatedNickname);
        Assertions.assertThat(userRepository.findById(saved.getId()).orElseThrow().getNickname())
                .isEqualTo(updatedNickname);

        String wsPayload = "잡 알림 ✅🔥";
        StandardWebSocketClient client = new StandardWebSocketClient();
        CompletableFuture<String> received = new CompletableFuture<>();
        WebSocketSession session = client.doHandshake(new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                session.sendMessage(new TextMessage(wsPayload));
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                received.complete(message.getPayload());
            }
        }, null, URI.create(String.format("ws://localhost:%d/ws/echo?token=%s", port, authResponse.getToken())))
                .get(5, TimeUnit.SECONDS);

        try {
            String echoed = received.get(5, TimeUnit.SECONDS);
            Assertions.assertThat(echoed).contains(wsPayload);
        } finally {
            session.close();
        }
    }

    private AuthResponse registerAndLogin(String nickname) {
        String username = "utf8" + System.nanoTime();
        RegisterRequest registerRequest = new RegisterRequest(username, "pass1234", nickname, null);
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                baseUrl("/api/auth/register"), registerRequest, AuthResponse.class);
        Assertions.assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        LoginRequest loginRequest = new LoginRequest(username, "pass1234");
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
