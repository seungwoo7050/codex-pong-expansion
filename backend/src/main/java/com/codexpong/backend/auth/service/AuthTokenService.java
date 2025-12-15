package com.codexpong.backend.auth.service;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/auth/service/AuthTokenService.java
 * 설명:
 *   - JWT 기반 인증 토큰을 생성하고 검증한다.
 *   - 유효한 토큰에서 사용자 식별자와 프로필 정보를 추출해 인증 객체를 만든다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/backend/v0.2.0-auth-and-profile.md
 * 변경 이력:
 *   - v0.2.0: 대칭키 기반 서명 및 만료 관리 추가
 */
@Service
public class AuthTokenService {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public AuthTokenService(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("username", user.username())
                .claim("nickname", user.nickname())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expires))
                .signWith(signingKey)
                .compact();
    }

    public Instant calculateExpiry() {
        return Instant.now().plusSeconds(expirationSeconds);
    }

    public Optional<AuthenticatedUser> parse(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = Long.parseLong(claims.getSubject());
            String username = claims.get("username", String.class);
            String nickname = claims.get("nickname", String.class);
            return Optional.of(new AuthenticatedUser(id, username, nickname));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
