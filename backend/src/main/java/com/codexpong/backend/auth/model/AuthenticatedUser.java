package com.codexpong.backend.auth.model;

import java.security.Principal;

/**
 * [도메인 모델] backend/src/main/java/com/codexpong/backend/auth/model/AuthenticatedUser.java
 * 설명:
 *   - JWT 검증 후 SecurityContext와 WebSocket 핸드셰이크 모두에서 재사용할 인증 사용자 표현이다.
 *   - Spring Security의 Principal을 구현하여 세션/토큰 기반 인증 객체로 활용한다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/backend/v0.2.0-auth-and-profile.md
 * 변경 이력:
 *   - v0.2.0: 사용자 ID/닉네임을 포함하는 기본 Principal 정의
 */
public record AuthenticatedUser(Long id, String username, String nickname) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
