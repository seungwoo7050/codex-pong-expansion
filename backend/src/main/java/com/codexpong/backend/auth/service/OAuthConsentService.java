package com.codexpong.backend.auth.service;

import com.codexpong.backend.auth.domain.OAuthConsent;
import com.codexpong.backend.auth.repository.OAuthConsentRepository;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/auth/service/OAuthConsentService.java
 * 설명:
 *   - OAuth 로그인 시 동의 스코프를 기록하고 조회한다.
 *   - 동일 사용자가 이미 동의한 경우 최신 기록을 유지하면서 append-only 정책을 따른다.
 */
@Service
public class OAuthConsentService {

    private static final Logger log = LoggerFactory.getLogger(OAuthConsentService.class);

    private final OAuthConsentRepository oauthConsentRepository;

    public OAuthConsentService(OAuthConsentRepository oauthConsentRepository) {
        this.oauthConsentRepository = oauthConsentRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasConsent(Long userId, String provider, Set<String> scopes) {
        return oauthConsentRepository.findTopByUserIdAndProviderOrderByConsentedAtDesc(userId, provider)
                .map(consent -> scopesMatch(consent, scopes))
                .orElse(false);
    }

    @Transactional
    public void recordConsent(Long userId, String provider, Set<String> scopes) {
        String scopeString = String.join(" ", scopes);
        oauthConsentRepository.save(new OAuthConsent(userId, provider, scopeString));
        log.info("[OAUTH_CONSENT] user={} provider={} scopes={}", userId, provider, scopeString);
    }

    private boolean scopesMatch(OAuthConsent consent, Set<String> scopes) {
        Set<String> existing = Arrays.stream(consent.getScopes().split(" "))
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toSet());
        return existing.containsAll(scopes);
    }
}
