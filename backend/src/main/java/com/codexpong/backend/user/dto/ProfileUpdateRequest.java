package com.codexpong.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [요청 DTO] backend/src/main/java/com/codexpong/backend/user/dto/ProfileUpdateRequest.java
 * 설명:
 *   - 내 프로필 수정 요청에서 닉네임과 아바타 정보를 전달한다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/backend/v0.2.0-auth-and-profile.md
 * 변경 이력:
 *   - v0.2.0: 기본 검증 제약 포함해 작성
 */
public class ProfileUpdateRequest {

    @NotBlank
    @Size(min = 2, max = 60)
    private String nickname;

    @Size(max = 255)
    private String avatarUrl;

    public ProfileUpdateRequest() {
    }

    public ProfileUpdateRequest(String nickname, String avatarUrl) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
