package com.codexpong.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [요청 DTO] backend/src/main/java/com/codexpong/backend/auth/dto/OAuthLoginRequest.java
 * 설명:
 *   - 카카오/네이버 OAuth 액세스 토큰을 받아 서버 측 프로필 조회 및 로그인에 사용한다.
 *   - 프런트엔드에서 인증 코드를 교환한 뒤 전달하는 토큰을 기준으로 동작한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 */
public record OAuthLoginRequest(
        @NotBlank(message = "액세스 토큰은 비어 있을 수 없습니다.") String accessToken
) {
}
