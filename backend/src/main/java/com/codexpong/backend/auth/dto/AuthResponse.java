package com.codexpong.backend.auth.dto;

import com.codexpong.backend.user.dto.UserResponse;
import java.time.Instant;

/**
 * [응답 DTO] backend/src/main/java/com/codexpong/backend/auth/dto/AuthResponse.java
 * 설명:
 *   - 로그인/회원가입 성공 시 발급된 토큰과 사용자 정보를 함께 반환한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 * 변경 이력:
 *   - v0.2.0: 토큰 만료 시각 포함 응답 구조 정의
 *   - v0.4.0: 레이팅이 포함된 사용자 응답 유지
 */
public class AuthResponse {

    private final String token;
    private final Instant expiresAt;
    private final UserResponse user;

    public AuthResponse(String token, Instant expiresAt, UserResponse user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public UserResponse getUser() {
        return user;
    }
}
