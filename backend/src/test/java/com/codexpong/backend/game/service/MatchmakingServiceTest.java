package com.codexpong.backend.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.codexpong.backend.game.GameResultService;
import com.codexpong.backend.game.service.MatchmakingService.MatchTicket;
import com.codexpong.backend.replay.ReplayService;
import com.codexpong.backend.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * [단위 테스트] backend/src/test/java/com/codexpong/backend/game/service/MatchmakingServiceTest.java
 * 설명:
 *   - 두 사용자가 빠른 대전 큐에 진입했을 때 매칭되고 roomId가 반환되는지 검증한다.
 */
class MatchmakingServiceTest {

    @Test
    @DisplayName("두 사용자가 대기열에 들어오면 즉시 매칭된다")
    void matchTwoPlayers() {
        GameResultService resultService = mock(GameResultService.class);
        ReplayService replayService = mock(ReplayService.class);
        GameRoomService roomService = new GameRoomService(resultService, replayService, new ObjectMapper());
        MatchmakingService matchmakingService = new MatchmakingService(roomService);

        User alice = new User("alice", "pass", "앨리스", null);
        User bob = new User("bob", "pass", "밥", null);
        ReflectionTestUtils.setField(alice, "id", 1L);
        ReflectionTestUtils.setField(bob, "id", 2L);

        MatchTicket first = matchmakingService.enqueue(alice, com.codexpong.backend.game.domain.MatchType.NORMAL);
        assertThat(first.status()).isEqualTo("WAITING");

        MatchTicket second = matchmakingService.enqueue(bob, com.codexpong.backend.game.domain.MatchType.NORMAL);
        assertThat(second.status()).isEqualTo("MATCHED");
        assertThat(second.roomId()).isNotNull();
    }
}
