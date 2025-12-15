package com.codexpong.backend.admin.dto;

import com.codexpong.backend.common.KstDateTime;
import com.codexpong.backend.user.domain.User;
import java.time.OffsetDateTime;

/**
 * [응답 DTO] backend/src/main/java/com/codexpong/backend/admin/dto/AdminUserResponse.java
 * 설명:
 *   - 관리자 콘솔에서 계정 상태와 기본 정보를 노출하기 위한 요약 DTO다.
 *   - 밴/정지 상태와 최근 뮤트 만료 시각을 함께 전달한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 * 변경 이력:
 *   - v0.9.0: 운영/관리자 응답 모델 추가
 *   - v0.10.0: 타임스탬프 OffsetDateTime 변환 및 로케일 필드 반영
 */
public record AdminUserResponse(
        Long id,
        String username,
        String nickname,
        Integer rating,
        boolean banned,
        String banReason,
        OffsetDateTime bannedAt,
        OffsetDateTime suspendedUntil,
        OffsetDateTime mutedUntil,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String authProvider,
        String locale
) {

    public static AdminUserResponse from(User user, java.time.LocalDateTime mutedUntil) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRating(),
                user.isBanned(),
                user.getBanReason(),
                KstDateTime.toOffset(user.getBannedAt()),
                KstDateTime.toOffset(user.getSuspendedUntil()),
                KstDateTime.toOffset(mutedUntil),
                KstDateTime.toOffset(user.getCreatedAt()),
                KstDateTime.toOffset(user.getUpdatedAt()),
                user.getAuthProvider(),
                user.getLocale()
        );
    }
}
