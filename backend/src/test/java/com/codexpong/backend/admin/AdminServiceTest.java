package com.codexpong.backend.admin;

import static org.junit.jupiter.api.Assertions.*;

import com.codexpong.backend.admin.dto.AdminUserResponse;
import com.codexpong.backend.admin.dto.ModerationRequest;
import com.codexpong.backend.admin.dto.ModerationRequest.ActionType;
import com.codexpong.backend.game.GameResult;
import com.codexpong.backend.game.GameResultRepository;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * [테스트] backend/src/test/java/com/codexpong/backend/admin/AdminServiceTest.java
 * 설명:
 *   - v0.9.0 관리자 서비스의 제재 처리와 통계 계산을 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameResultRepository gameResultRepository;

    @Test
    @DisplayName("밴과 정지, 뮤트 적용 시 사용자 상태가 업데이트된다")
    void moderateUserUpdatesStatus() {
        User target = userRepository.save(new User("admin-target", "pass", "관리대상", null));

        ModerationRequest banRequest = buildRequest(ActionType.BAN, 0, "테스트 밴");
        AdminUserResponse banned = adminService.moderate(target.getId(), banRequest);
        assertTrue(banned.banned(), "밴 플래그가 설정되어야 한다");
        assertEquals("테스트 밴", banned.banReason(), "밴 사유가 보존되어야 한다");

        ModerationRequest suspendRequest = buildRequest(ActionType.SUSPEND, 30, "30분 정지");
        AdminUserResponse suspended = adminService.moderate(target.getId(), suspendRequest);
        assertNotNull(suspended.suspendedUntil(), "정지 종료 시각이 기록되어야 한다");

        ModerationRequest muteRequest = buildRequest(ActionType.MUTE, 10, "채팅 제재");
        AdminUserResponse muted = adminService.moderate(target.getId(), muteRequest);
        assertNotNull(muted.mutedUntil(), "뮤트 만료 시각이 포함되어야 한다");
    }

    @Test
    @DisplayName("통계 API는 사용자 수와 경기 수를 반환한다")
    void statsReturnsCounts() {
        User playerA = userRepository.save(new User("user-a", "pass", "사용자A", null));
        User playerB = userRepository.save(new User("user-b", "pass", "사용자B", null));
        GameResult result = new GameResult(playerA, playerB, 5, 3, "room-1", MatchType.NORMAL,
                0, 0, 1200, 1200, LocalDateTime.now(), LocalDateTime.now());
        gameResultRepository.save(result);

        var stats = adminService.stats();

        assertTrue(stats.userCount() >= 2, "테스트 데이터 사용자 수가 반영되어야 한다");
        assertTrue(stats.totalMatches() >= 1, "경기 카운트가 1 이상이어야 한다");
        assertEquals(0, stats.activeGames(), "테스트 환경에서는 활성 경기가 없어야 한다");
    }

    private ModerationRequest buildRequest(ActionType action, int durationMinutes, String reason) {
        ModerationRequest request = new ModerationRequest();
        try {
            var actionField = ModerationRequest.class.getDeclaredField("action");
            actionField.setAccessible(true);
            actionField.set(request, action);
            var durationField = ModerationRequest.class.getDeclaredField("durationMinutes");
            durationField.setAccessible(true);
            durationField.set(request, durationMinutes);
            var reasonField = ModerationRequest.class.getDeclaredField("reason");
            reasonField.setAccessible(true);
            reasonField.set(request, reason);
        } catch (Exception ignored) {
        }
        return request;
    }
}
