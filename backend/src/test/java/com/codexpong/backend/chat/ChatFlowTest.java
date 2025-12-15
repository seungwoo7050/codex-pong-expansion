package com.codexpong.backend.chat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codexpong.backend.auth.dto.RegisterRequest;
import com.codexpong.backend.chat.service.ChatModerationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/chat/ChatFlowTest.java
 * 설명:
 *   - DM, 로비, 매치 채팅 REST 흐름과 뮤트 제한이 동작하는지 검증한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/backend/v0.6.0-chat-and-channels.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatModerationService chatModerationService;

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private String aliceToken;
    private String bobToken;
    private Long aliceId;
    private Long bobId;

    @BeforeEach
    void setupUsers() throws Exception {
        String aliceUsername = "alice" + COUNTER.incrementAndGet();
        String bobUsername = "bob" + COUNTER.incrementAndGet();

        Map<String, Object> alice = register(aliceUsername, "앨리스");
        aliceToken = (String) alice.get("token");
        Map<String, Object> aliceUser = (Map<String, Object>) alice.get("user");
        aliceId = ((Number) aliceUser.get("id")).longValue();

        Map<String, Object> bob = register(bobUsername, "밥이");
        bobToken = (String) bob.get("token");
        Map<String, Object> bobUser = (Map<String, Object>) bob.get("user");
        bobId = ((Number) bobUser.get("id")).longValue();
    }

    @Test
    void DM과_로비_매치_채팅과_뮤트제한을_검증한다() throws Exception {
        mockMvc.perform(post("/api/chat/dm/" + bobId)
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "안녕 밥"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientId", is(bobId.intValue())));

        mockMvc.perform(get("/api/chat/dm/" + bobId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].content", containsString("안녕")));

        mockMvc.perform(post("/api/chat/lobby")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "로비 공지"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channelType", is("LOBBY")));

        mockMvc.perform(get("/api/chat/lobby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].content", containsString("로비")));

        mockMvc.perform(post("/api/chat/match/room-1")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "방 채팅"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channelKey", is("room-1")));

        mockMvc.perform(get("/api/chat/match/room-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].content", containsString("방")));

        chatModerationService.muteUser(aliceId, "테스트 뮤트", LocalDateTime.now().plusMinutes(5));

        mockMvc.perform(post("/api/chat/lobby")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "뮤트 확인"))))
                .andExpect(status().isForbidden());
    }

    private Map<String, Object> register(String username, String nickname) throws Exception {
        RegisterRequest request = new RegisterRequest(username, "password123", nickname, null);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
    }
}
