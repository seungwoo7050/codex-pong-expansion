package com.codexpong.backend.admin.dto;

/**
 * [응답 DTO] backend/src/main/java/com/codexpong/backend/admin/dto/AdminStatsResponse.java
 * 설명:
 *   - 관리자 콘솔에서 시스템 상태를 한 눈에 확인하기 위한 지표 묶음이다.
 *   - 사용자 수, 누적 경기 수, 활성 경기/관전자 수를 포함한다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/backend/v0.9.0-admin-and-ops.md
 */
public record AdminStatsResponse(
        long userCount,
        long totalMatches,
        int activeGames,
        int activeSpectators
) {
}
