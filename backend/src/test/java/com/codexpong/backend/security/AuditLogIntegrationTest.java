package com.codexpong.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codexpong.backend.admin.AdminService;
import com.codexpong.backend.admin.dto.ModerationRequest;
import com.codexpong.backend.admin.dto.ModerationRequest.ActionType;
import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.security.audit.AuditLog;
import com.codexpong.backend.security.audit.AuditLogRepository;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/security/AuditLogIntegrationTest.java
 * 설명:
 *   - 관리자 제재 요청 시 append-only 감사 로그가 남는지 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditLogIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("관리자 제재 요청이 감사 로그에 기록된다")
    void adminModerationIsAudited() {
        User admin = userRepository.save(new User("audit-admin", "pass", "감사자", null));
        User target = userRepository.save(new User("audit-target", "pass", "대상", null));
        AuthenticatedUser actor = new AuthenticatedUser(admin.getId(), admin.getUsername(), admin.getNickname());

        int before = auditLogRepository.findAll().size();
        ModerationRequest request = buildRequest(ActionType.MUTE, 5, "도배");
        adminService.moderate(actor, target.getId(), request);

        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(before + 1, logs.size(), "새 감사 로그가 추가되어야 한다");
        AuditLog log = logs.get(logs.size() - 1);
        assertEquals("USER_MODERATION", log.getAction());
        assertEquals(admin.getId(), log.getActorId());
        assertEquals(target.getId().toString(), log.getTargetId());
        assertTrue(log.getDetail().contains("MUTE"));
        assertNotNull(log.getOccurredAt());
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
