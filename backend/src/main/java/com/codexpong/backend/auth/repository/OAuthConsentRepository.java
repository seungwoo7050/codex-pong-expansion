package com.codexpong.backend.auth.repository;

import com.codexpong.backend.auth.domain.OAuthConsent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [저장소] backend/src/main/java/com/codexpong/backend/auth/repository/OAuthConsentRepository.java
 * 설명:
 *   - OAuth 동의 내역을 조회/저장한다.
 */
public interface OAuthConsentRepository extends JpaRepository<OAuthConsent, Long> {

    Optional<OAuthConsent> findTopByUserIdAndProviderOrderByConsentedAtDesc(Long userId, String provider);
}
