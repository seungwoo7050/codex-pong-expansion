package com.codexpong.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [요청 DTO] backend/src/main/java/com/codexpong/backend/auth/dto/LoginRequest.java
 * 설명:
 *   - 로그인 시 아이디와 비밀번호를 전달하기 위한 최소 구조다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/backend/v0.2.0-auth-and-profile.md
 * 변경 이력:
 *   - v0.2.0: 로그인 요청 DTO 추가
 */
public class LoginRequest {

    @NotBlank
    @Size(min = 4, max = 60)
    private String username;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
