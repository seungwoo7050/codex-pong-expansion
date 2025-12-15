package com.codexpong.backend.replay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codexpong.backend.game.GameResult;
import com.codexpong.backend.game.GameResultRepository;
import com.codexpong.backend.game.domain.GameRoom;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.game.engine.model.GameSnapshot;
import com.codexpong.backend.chat.repository.ChatMessageRepository;
import com.codexpong.backend.chat.repository.ChatMuteRepository;
import com.codexpong.backend.job.JobRepository;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/replay/ReplayControllerTest.java
 * 설명:
 *   - v0.11.0 리플레이 목록/상세/이벤트 스트림 API의 권한 및 응답 형식을 검증한다.
 *   - 소유자만 접근 가능하도록 제한하며 녹화 버퍼로 생성한 JSONL 파일을 내려받을 수 있는지 확인한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/backend/v0.11.0-replay-recording-and-storage.md
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReplayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReplayService replayService;

    @Autowired
    private ReplayRepository replayRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameResultRepository gameResultRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatMuteRepository chatMuteRepository;

    @Autowired
    private JobRepository jobRepository;

    @BeforeEach
    void cleanUp() {
        jobRepository.deleteAll();
        replayRepository.deleteAll();
        gameResultRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatMuteRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("소유자는 리플레이 메타데이터와 이벤트 파일을 조회할 수 있다")
    void ownerCanBrowseReplay() throws Exception {
        String uniqueSuffix = String.valueOf(System.nanoTime());
        String ownerUsername = "replay-owner-" + uniqueSuffix;
        String opponentUsername = "replay-opponent-" + uniqueSuffix;

        String ownerToken = obtainToken(ownerUsername);
        obtainToken(opponentUsername);
        User owner = userRepository.findByUsername(ownerUsername).orElseThrow();
        User opponent = userRepository.findByUsername(opponentUsername).orElseThrow();

        GameRoom room = new GameRoom(owner, opponent, MatchType.NORMAL);
        replayService.startRecording(room);
        replayService.appendSnapshot(room.getRoomId(), room.currentSnapshot());
        replayService.appendSnapshot(room.getRoomId(), new GameSnapshot(room.getRoomId(), 0, 0, 0, 0, 10, 20, 5, 3, 5, true));

        GameResult result = gameResultRepository.save(new GameResult(owner, opponent, 5, 3, room.getRoomId(),
                MatchType.NORMAL, 0, 0, owner.getRating(), opponent.getRating(),
                LocalDateTime.now(), LocalDateTime.now()));
        replayService.completeRecording(room, result);
        Replay replay = replayRepository.findByOwnerOrderByCreatedAtDesc(owner).get(0);

        mockMvc.perform(get("/api/replays")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(mvcResult -> assertThat(mvcResult.getResponse().getContentAsString())
                        .contains("\"replayId\":" + replay.getId()));

        mockMvc.perform(get("/api/replays/" + replay.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(mvcResult -> assertThat(mvcResult.getResponse().getContentAsString())
                        .contains(replay.getChecksum()));

        mockMvc.perform(get("/api/matches/" + result.getId() + "/replay")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/replays/" + replay.getId() + "/events")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(mvcResult -> assertThat(mvcResult.getResponse().getContentType())
                        .isEqualTo("application/x-ndjson"));
    }

    private String obtainToken(String username) throws Exception {
        Map<String, String> registerPayload = Map.of(
                "username", username,
                "password", "password123",
                "nickname", "사용자",
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
