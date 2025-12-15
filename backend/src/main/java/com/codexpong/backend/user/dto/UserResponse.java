package com.codexpong.backend.user.dto;

import com.codexpong.backend.common.KstDateTime;
import com.codexpong.backend.user.domain.User;
import java.time.OffsetDateTime;

/**
 * [응답 DTO] backend/src/main/java/com/codexpong/backend/user/dto/UserResponse.java
 * 설명:
 *   - 사용자 프로필과 계정 메타 정보를 클라이언트에 전달하기 위한 DTO다.
 *   - v0.4.0에서 랭크 레이팅을 추가해 프로필 UI와 리더보드에서 재사용한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 * 변경 이력:
 *   - v0.2.0: 기본 필드 매핑 추가
 *   - v0.4.0: rating 필드 추가
 *   - v0.10.0: KST OffsetDateTime 기반 타임스탬프 변환 추가
 */
public class UserResponse {

    private final Long id;
    private final String username;
    private final String nickname;
    private final String avatarUrl;
    private final Integer rating;
    private final String authProvider;
    private final String locale;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public UserResponse(Long id, String username, String nickname, String avatarUrl, Integer rating,
            String authProvider, String locale,
            OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.rating = rating;
        this.authProvider = authProvider;
        this.locale = locale;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRating(),
                user.getAuthProvider(),
                user.getLocale(),
                KstDateTime.toOffset(user.getCreatedAt()),
                KstDateTime.toOffset(user.getUpdatedAt())
        );
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Integer getRating() {
        return rating;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public String getLocale() {
        return locale;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
