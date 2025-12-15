package com.codexpong.backend.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * [엔티티] backend/src/main/java/com/codexpong/backend/auth/domain/OAuthConsent.java
 * 설명:
 *   - OAuth 로그인 시 사용자가 위임한 스코프를 기록해 추적 가능하도록 한다.
 *   - v1.5.0 보안 요구사항에 맞춰 동의 내역을 저장하며, 수정 없이 누적 저장한다.
 */
@Entity
@Table(name = "oauth_consents")
public class OAuthConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, updatable = false)
    private String provider;

    @Column(nullable = false, updatable = false, length = 300)
    private String scopes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime consentedAt;

    protected OAuthConsent() {
    }

    public OAuthConsent(Long userId, String provider, String scopes) {
        this.userId = userId;
        this.provider = provider;
        this.scopes = scopes;
        this.consentedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getScopes() {
        return scopes;
    }

    public LocalDateTime getConsentedAt() {
        return consentedAt;
    }
}
