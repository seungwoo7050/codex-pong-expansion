package com.codexpong.backend.security.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/security/audit/AuditLog.java
 * 설명:
 *   - 관리자 및 민감 액션을 감시하기 위한 append-only 감사 로그 레코드다.
 *   - 모든 필드는 생성 시에만 설정하며, 이후 수정/삭제를 허용하지 않는 정책을 따른다.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(nullable = false, updatable = false)
    private Long actorId;

    @Column(nullable = false, updatable = false)
    private String targetType;

    @Column(nullable = false, updatable = false)
    private String targetId;

    @Column(nullable = false, updatable = false, length = 500)
    private String detail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    protected AuditLog() {
    }

    public AuditLog(String action, Long actorId, String targetType, String targetId, String detail) {
        this.action = action;
        this.actorId = actorId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
        this.occurredAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public Long getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
