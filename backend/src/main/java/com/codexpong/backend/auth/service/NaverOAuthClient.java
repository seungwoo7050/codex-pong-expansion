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
 * [통신 모듈] backend/src/main/java/com/codexpong/backend/auth/service/NaverOAuthClient.java
 * 설명:
 *   - 네이버 OAuth 액세스 토큰으로 사용자 프로필을 조회한다.
 *   - 성공 시 공통 OAuthProfile로 매핑하고, 실패 시 인증 오류를 반환한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 */
@Component
public class NaverOAuthClient {

    private final RestTemplate restTemplate;
    private final String profileUri;

    public NaverOAuthClient(@Qualifier("naverOAuthRestTemplate") RestTemplate restTemplate,
            @Value("${auth.naver.profile-uri:https://openapi.naver.com/v1/nid/me}") String profileUri) {
        this.restTemplate = restTemplate;
        this.profileUri = profileUri;
    }

    public OAuthProfile fetchProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<NaverProfileResponse> response = restTemplate.exchange(
                profileUri,
                HttpMethod.GET,
                request,
                NaverProfileResponse.class
        );
        NaverProfileResponse body = response.getBody();
        if (body == null || body.response() == null || body.response().id() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "네이버 프로필 응답이 올바르지 않습니다.");
        }
        if (!"00".equals(body.resultcode())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "네이버 인증에 실패했습니다.");
        }
        String nickname = Optional.ofNullable(body.response().nickname()).orElse("네이버 유저");
        String email = body.response().email();
        String avatarUrl = body.response().profileImage();
        return new OAuthProfile("naver", body.response().id(), nickname, email, "ko-KR", avatarUrl);
    }

    /**
     * 네이버 프로필 응답 레코드 매핑.
     */
    public record NaverProfileResponse(
            String resultcode,
            String message,
            NaverProfile response
    ) {
        public record NaverProfile(
                String id,
                String nickname,
                String email,
                @JsonProperty("profile_image") String profileImage
        ) {
        }
    }
}
