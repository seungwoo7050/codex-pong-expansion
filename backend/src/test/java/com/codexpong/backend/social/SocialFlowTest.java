package com.codexpong.backend.social;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codexpong.backend.auth.dto.RegisterRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * [통합 테스트] src/test/java/com/codexpong/backend/social/SocialFlowTest.java
 * 설명:
 *   - 친구 요청/수락, 차단/해제, 초대 수락까지 v0.5.0 소셜 흐름이 동작하는지 검증한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final AtomicInteger COUNTER = new AtomicInteger(1000);
    private String aliceToken;
    private String bobToken;
    private String aliceUsername;
    private String bobUsername;
    private Long aliceId;
    private Long bobId;

    @BeforeEach
    void setupUsers() throws Exception {
        aliceUsername = "alice" + COUNTER.incrementAndGet();
        bobUsername = "bob" + COUNTER.incrementAndGet();

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
    void 친구요청_수락과_차단_초대까지_흐름을_처리한다() throws Exception {
        MvcResult requestResult = mockMvc.perform(post("/api/social/friend-requests")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetUsername", bobUsername))))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> requestBody = objectMapper.readValue(requestResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        Integer requestId = (Integer) requestBody.get("id");

        mockMvc.perform(post("/api/social/friend-requests/" + requestId + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/social/friends")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(bobId.intValue())));

        mockMvc.perform(post("/api/social/blocks")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetUsername", aliceUsername))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(aliceId.intValue())));

        mockMvc.perform(post("/api/social/friend-requests")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetUsername", bobUsername))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/social/blocks/" + aliceId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        MvcResult reRequest = mockMvc.perform(post("/api/social/friend-requests")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetUsername", aliceUsername))))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> reRequestBody = objectMapper.readValue(reRequest.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        Integer reRequestId = (Integer) reRequestBody.get("id");

        mockMvc.perform(post("/api/social/friend-requests/" + reRequestId + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());

        MvcResult inviteResult = mockMvc.perform(post("/api/social/invites")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("targetUserId", bobId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiverId", is(bobId.intValue())))
                .andReturn();

        Map<String, Object> inviteBody = objectMapper.readValue(inviteResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        Integer inviteId = (Integer) inviteBody.get("id");

        mockMvc.perform(post("/api/social/invites/" + inviteId + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")))
                .andExpect(jsonPath("$.roomId").exists());
    }

    private Map<String, Object> register(String username, String nickname) throws Exception {
        RegisterRequest request = new RegisterRequest(username, "password123", nickname, null);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        if (status != 200) {
            throw new AssertionError("회원가입 실패 응답(" + status + "): " + body);
        }
        return objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
    }
}
