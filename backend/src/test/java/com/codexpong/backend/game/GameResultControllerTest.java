package com.codexpong.backend.game;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * [테스트] backend/src/test/java/com/codexpong/backend/game/GameResultControllerTest.java
 * 설명:
 *   - v0.3.0에서 조회 전용으로 전환된 경기 기록 API가 인증 후 접근 가능한지 검증한다.
 * 버전: v0.3.0
 * 관련 설계문서:
 *   - design/backend/v0.3.0-game-and-matchmaking.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GameResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 인증된_사용자가_경기_기록을_조회할_수_있다() throws Exception {
        String token = obtainToken("record-viewer");

        mockMvc.perform(get("/api/games")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String obtainToken(String username) throws Exception {
        Map<String, String> registerPayload = Map.of(
                "username", username,
                "password", "password123",
                "nickname", "게스트",
                "avatarUrl", ""
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isOk());

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> response = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        return (String) response.get("token");
    }
}
