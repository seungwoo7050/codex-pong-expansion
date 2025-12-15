package com.codexpong.backend.security.audit;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/security/audit/AuditLogService.java
 * 설명:
 *   - 감사 로그를 기록하는 공용 진입점으로, append-only 정책을 준수한다.
 *   - 민감 액션마다 actor/target/context를 남겨 추적 가능하도록 한다.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void recordAdminAction(AuthenticatedUser actor, String action, String targetType, String targetId,
            String detail) {
        if (actor == null) {
            throw new IllegalArgumentException("감사 로그 기록 시 actor가 필요합니다.");
        }
        AuditLog saved = auditLogRepository.save(new AuditLog(action, actor.id(), targetType, targetId, detail));
        log.info("[AUDIT] action={} actor={} targetType={} targetId={} occurredAt={}",
                saved.getAction(), saved.getActorId(), saved.getTargetType(), saved.getTargetId(), saved.getOccurredAt());
    }
}
