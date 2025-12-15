package com.codexpong.backend.auth.model;

/**
 * [모델] backend/src/main/java/com/codexpong/backend/auth/model/OAuthProfile.java
 * 설명:
 *   - OAuth 공급자에서 받아온 사용자 식별자와 기본 정보를 담는다.
 *   - User 엔티티 생성 혹은 기존 계정 조회 시 공통으로 활용한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 */
public record OAuthProfile(
        String provider,
        String providerUserId,
        String nickname,
        String email,
        String locale,
        String avatarUrl
) {
}
