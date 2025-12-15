package com.codexpong.backend.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codexpong.backend.game.GameResultService;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.replay.ReplayService;
import com.codexpong.backend.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketSession;

/**
 * [단위 테스트] backend/src/test/java/com/codexpong/backend/game/service/GameRoomServiceSpectatorTest.java
 * 설명:
 *   - 관전자 한도 및 등록 로직이 설정값을 준수하는지 검증한다.
 * 버전: v0.8.0
 * 관련 설계문서:
 *   - design/backend/v0.8.0-spectator-mode.md
 */
class GameRoomServiceSpectatorTest {

    @Test
    @DisplayName("관전자 한도가 초과되면 등록에 실패한다")
    void spectatorLimit() {
        GameResultService resultService = mock(GameResultService.class);
        ReplayService replayService = mock(ReplayService.class);
        GameRoomService roomService = new GameRoomService(resultService, replayService, new ObjectMapper());

        User left = new User("left", "pass", "왼쪽", null);
        User right = new User("right", "pass", "오른쪽", null);
        ReflectionTestUtils.setField(left, "id", 1L);
        ReflectionTestUtils.setField(right, "id", 2L);

        var room = roomService.createRoom(left, right, MatchType.NORMAL);

        for (int i = 0; i < 30; i++) {
            WebSocketSession session = mock(WebSocketSession.class);
            when(session.getId()).thenReturn("spec-" + i);
            assertThat(roomService.registerSpectatorSession(room, session.getId(), session)).isTrue();
        }
        WebSocketSession blocked = mock(WebSocketSession.class);
        when(blocked.getId()).thenReturn("spec-over");
        assertThat(roomService.registerSpectatorSession(room, blocked.getId(), blocked)).isFalse();
    }
}
