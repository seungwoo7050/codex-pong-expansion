package com.codexpong.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [요청 DTO] backend/src/main/java/com/codexpong/backend/auth/dto/RegisterRequest.java
 * 설명:
 *   - 회원가입 시 필요한 최소 정보(아이디, 비밀번호, 닉네임, 아바타)를 전달한다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/backend/v0.2.0-auth-and-profile.md
 * 변경 이력:
 *   - v0.2.0: 필수 필드와 기본 검증 규칙 정의
 */
public class RegisterRequest {

    @NotBlank
    @Size(min = 4, max = 60)
    private String username;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    @NotBlank
    @Size(min = 2, max = 60)
    private String nickname;

    @Size(max = 255)
    private String avatarUrl;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String password, String nickname, String avatarUrl) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
