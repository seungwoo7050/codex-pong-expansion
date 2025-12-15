package com.codexpong.backend.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codexpong.backend.auth.dto.AuthResponse;
import com.codexpong.backend.auth.dto.RegisterRequest;
import com.codexpong.backend.auth.service.AuthService;
import com.codexpong.backend.chat.dto.ChatSendRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/security/AuthParityIntegrationTest.java
 * 설명:
 *   - REST와 WebSocket 모두에서 JWT 인증이 동일하게 요구되는지 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthParityIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private AuthResponse authResponse;
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @BeforeEach
    void registerUser() {
        String username = "ws-parity" + COUNTER.incrementAndGet();
        RegisterRequest request = new RegisterRequest(username, "password123", "웹소켓", null);
        authResponse = authService.register(request);
    }

    @Test
    @DisplayName("REST와 WebSocket 모두에서 잘못된 토큰을 거부한다")
    void restAndWebSocketRequireValidToken() throws Exception {
        ChatSendRequest chatRequest = new ChatSendRequest();
        chatRequest.setContent("hello");

        mockMvc.perform(post("/api/chat/lobby")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/chat/lobby")
                        .header("Authorization", "Bearer " + authResponse.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk());

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        WebSocketSession session = client.doHandshake(new AbstractWebSocketHandler() {
        }, headers, URI.create("ws://localhost:" + port + "/ws/chat?token=" + authResponse.getToken()))
                .completable()
                .get(3, TimeUnit.SECONDS);
        assertNotNull(session);
        session.close();

        assertThrows(ExecutionException.class, () -> client.doHandshake(new AbstractWebSocketHandler() {
                }, headers, URI.create("ws://localhost:" + port + "/ws/chat?token=invalid-token"))
                        .completable()
                        .get(3, TimeUnit.SECONDS));
    }
}
