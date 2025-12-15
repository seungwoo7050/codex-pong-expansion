package com.codexpong.backend.auth.service;

import com.codexpong.backend.auth.dto.AuthResponse;
import com.codexpong.backend.auth.model.OAuthProfile;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/auth/service/OAuthLoginService.java
 * 설명:
 *   - 카카오/네이버 OAuth 프로필을 조회하고 로컬 사용자 계정에 매핑한다.
 *   - 최초 로그인 시 공급자 식별자를 기반으로 계정을 생성하고, 이후에는 동일 계정으로 토큰을 발급한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 */
@Service
public class OAuthLoginService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final NaverOAuthClient naverOAuthClient;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public OAuthLoginService(KakaoOAuthClient kakaoOAuthClient, NaverOAuthClient naverOAuthClient,
            UserRepository userRepository, PasswordEncoder passwordEncoder, AuthService authService) {
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.naverOAuthClient = naverOAuthClient;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    public AuthResponse loginWithKakao(String accessToken) {
        OAuthProfile profile = kakaoOAuthClient.fetchProfile(accessToken);
        return handleProfile(profile);
    }

    public AuthResponse loginWithNaver(String accessToken) {
        OAuthProfile profile = naverOAuthClient.fetchProfile(accessToken);
        return handleProfile(profile);
    }

    private AuthResponse handleProfile(OAuthProfile profile) {
        User user = userRepository.findByAuthProviderAndProviderUserId(profile.provider(), profile.providerUserId())
                .orElseGet(() -> createUser(profile));
        authService.ensureActive(user);
        return authService.toAuthResponse(user);
    }

    private User createUser(OAuthProfile profile) {
        if (profile.provider() == null || profile.providerUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제공자 정보가 누락되었습니다.");
        }
        String placeholderPassword = passwordEncoder.encode("oauth-" + UUID.randomUUID());
        User user = User.oauth(
                profile.provider(),
                profile.providerUserId(),
                profile.nickname(),
                profile.email(),
                profile.locale(),
                placeholderPassword,
                profile.avatarUrl()
        );
        return userRepository.save(user);
    }
}
