package com.codexpong.backend.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.game.service.GameRoomService;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/game/SpectatorControllerTest.java
 * 설명:
 *   - 진행 중인 경기 목록 API가 관전자 입장 정보를 반환하는지 검증한다.
 * 버전: v0.8.0
 * 관련 설계문서:
 *   - design/backend/v0.8.0-spectator-mode.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpectatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 진행중_경기_목록에_roomId와_관전자_제한이_포함된다() throws Exception {
        String playerA = "spec-a-" + UUID.randomUUID();
        String playerB = "spec-b-" + UUID.randomUUID();
        String token = registerAndLogin(playerA);
        registerAndLogin(playerB);

        User userA = userRepository.findByUsername(playerA).orElseThrow();
        User userB = userRepository.findByUsername(playerB).orElseThrow();

        gameRoomService.createRoom(userA, userB, MatchType.NORMAL);

        var result = mockMvc.perform(get("/api/match/ongoing")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> payload = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertThat(payload).isNotEmpty();
        Map<String, Object> room = payload.get(0);
        assertThat(room.get("roomId")).isNotNull();
        assertThat(room.get("spectatorLimit")).isNotNull();
    }

    private String registerAndLogin(String username) throws Exception {
        Map<String, String> registerPayload = Map.of(
                "username", username,
                "password", "password123",
                "nickname", "관전자테스트",
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
