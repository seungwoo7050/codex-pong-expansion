package com.codexpong.backend.auth.service;

import com.codexpong.backend.auth.model.OAuthProfile;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * [통신 모듈] backend/src/main/java/com/codexpong/backend/auth/service/KakaoOAuthClient.java
 * 설명:
 *   - 카카오 OAuth 액세스 토큰으로 사용자 프로필을 조회한다.
 *   - 실패 시 401 상태를 던져 프런트엔드가 재인증을 유도하도록 한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 */
@Component
public class KakaoOAuthClient {

    private final RestTemplate restTemplate;
    private final String profileUri;

    public KakaoOAuthClient(@Qualifier("kakaoOAuthRestTemplate") RestTemplate restTemplate,
            @Value("${auth.kakao.profile-uri:https://kapi.kakao.com/v2/user/me}") String profileUri) {
        this.restTemplate = restTemplate;
        this.profileUri = profileUri;
    }

    public OAuthProfile fetchProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<KakaoProfileResponse> response = restTemplate.exchange(
                profileUri,
                HttpMethod.GET,
                request,
                KakaoProfileResponse.class
        );
        KakaoProfileResponse body = response.getBody();
        if (body == null || body.kakaoAccount() == null || body.kakaoAccount().profile() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 프로필 응답이 올바르지 않습니다.");
        }
        String nickname = Optional.ofNullable(body.kakaoAccount().profile().nickname()).orElse("카카오 유저");
        String email = body.kakaoAccount().email();
        String avatarUrl = Optional.ofNullable(body.kakaoAccount().profile().thumbnailImageUrl()).orElse(null);
        return new OAuthProfile("kakao", String.valueOf(body.id()), nickname, email, "ko-KR", avatarUrl);
    }

    /**
     * 카카오 API 응답을 최소 필드만 맵핑한 레코드.
     */
    public record KakaoProfileResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
        public record KakaoAccount(
                Profile profile,
                String email
        ) {
            public record Profile(
                    String nickname,
                    @JsonProperty("thumbnail_image_url") String thumbnailImageUrl
            ) {
            }
        }
    }
}
