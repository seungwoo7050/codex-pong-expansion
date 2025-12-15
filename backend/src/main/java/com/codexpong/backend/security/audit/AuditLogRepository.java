package com.codexpong.backend.security.audit;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [저장소] backend/src/main/java/com/codexpong/backend/security/audit/AuditLogRepository.java
 * 설명:
 *   - 감사 로그를 append-only로 보관하기 위한 JPA 리포지토리다.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
