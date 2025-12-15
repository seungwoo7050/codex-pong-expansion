package com.codexpong.backend.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/game/RankingFlowTest.java
 * 설명:
 *   - 랭크 경기 결과가 저장되면 레이팅이 변동되고 리더보드에 반영되는지 검증한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RankingFlowTest {

    @Autowired
    private GameResultService gameResultService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 랭크_경기_결과가_레이팅과_리더보드에_반영된다() throws Exception {
        String playerA = "rankA-" + UUID.randomUUID();
        String playerB = "rankB-" + UUID.randomUUID();
        String tokenA = registerAndLogin(playerA);
        registerAndLogin(playerB);

        User userA = userRepository.findByUsername(playerA).orElseThrow();
        User userB = userRepository.findByUsername(playerB).orElseThrow();

        gameResultService.recordResult("rank-room", userA, userB, 7, 3, MatchType.RANKED, LocalDateTime.now(),
                LocalDateTime.now());

        User updatedA = userRepository.findByUsername(playerA).orElseThrow();
        User updatedB = userRepository.findByUsername(playerB).orElseThrow();

        assertThat(updatedA.getRating()).isGreaterThan(updatedB.getRating());
        assertThat(updatedA.getRating()).isGreaterThanOrEqualTo(1200);
        assertThat(updatedB.getRating()).isLessThanOrEqualTo(1200);

        var leaderboardResult = mockMvc.perform(get("/api/rank/leaderboard")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        List<Map<String, Object>> leaderboard = objectMapper.readValue(
                leaderboardResult.getResponse().getContentAsString(), new TypeReference<>() {
                });
        Number topUserId = (Number) leaderboard.get(0).get("userId");
        assertThat(topUserId.longValue()).isEqualTo(updatedA.getId());
    }

    private String registerAndLogin(String username) throws Exception {
        Map<String, String> registerPayload = Map.of(
                "username", username,
                "password", "password123",
                "nickname", "랭크유저",
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
