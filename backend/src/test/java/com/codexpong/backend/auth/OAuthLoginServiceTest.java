package com.codexpong.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.codexpong.backend.auth.dto.AuthResponse;
import com.codexpong.backend.auth.service.OAuthLoginService;
import com.codexpong.backend.auth.service.AuthTokenService;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;

/**
 * [테스트] OAuth 로그인 흐름을 검증한다.
 * 설명:
 *   - 카카오/네이버 액세스 토큰으로 프로필을 조회하고 계정이 생성·재사용되는지 확인한다.
 *   - 반환된 JWT가 파싱 가능한지와 KST 타임스탬프가 포함되는지 점검한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockRestServiceServer
class OAuthLoginServiceTest {

    @Autowired
    private OAuthLoginService oauthLoginService;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Qualifier("kakaoOAuthRestTemplate")
    private RestTemplate kakaoRestTemplate;

    @Autowired
    @Qualifier("naverOAuthRestTemplate")
    private RestTemplate naverRestTemplate;

    @BeforeEach
    void cleanUsers() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("카카오 토큰으로 신규 사용자를 생성하고 JWT를 반환한다")
    void kakaoOAuthLoginCreatesUser() {
        MockRestServiceServer kakaoServer = MockRestServiceServer.bindTo(kakaoRestTemplate).ignoreExpectOrder(true).build();
        kakaoServer.expect(requestTo("http://localhost/kakao-profile"))
                .andRespond(withSuccess("{" +
                        "\"id\":12345," +
                        "\"kakao_account\":{\"email\":\"kakao@test.com\",\"profile\":{\"nickname\":\"카카오유저\",\"thumbnail_image_url\":\"http://img.test/kakao.png\"}}" +
                        "}", MediaType.APPLICATION_JSON));

        AuthResponse response = oauthLoginService.loginWithKakao("dummy-token");

        kakaoServer.verify();
        User saved = userRepository.findByAuthProviderAndProviderUserId("kakao", "12345").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("kakao@test.com");
        assertThat(response.getUser().getAuthProvider()).isEqualTo("kakao");
        assertThat(response.getUser().getCreatedAt().getOffset().getId()).isEqualTo("+09:00");
        assertThat(authTokenService.parse(response.getToken())).isPresent();
    }

    @Test
    @DisplayName("네이버 토큰으로 기존 계정을 재사용한다")
    void naverOAuthLoginReusesUser() {
        MockRestServiceServer naverServer = MockRestServiceServer.bindTo(naverRestTemplate).ignoreExpectOrder(true).build();
        naverServer.expect(requestTo("http://localhost/naver-profile"))
                .andRespond(withSuccess("{" +
                        "\"resultcode\":\"00\",\"message\":\"success\",\"response\":{\"id\":\"naver-1\",\"nickname\":\"네이버인증\",\"email\":\"naver@test.com\",\"profile_image\":\"http://img.test/naver.png\"}}" +
                        "}", MediaType.APPLICATION_JSON));
        naverServer.expect(requestTo("http://localhost/naver-profile"))
                .andRespond(withSuccess("{" +
                        "\"resultcode\":\"00\",\"message\":\"success\",\"response\":{\"id\":\"naver-1\",\"nickname\":\"네이버인증\",\"email\":\"naver@test.com\",\"profile_image\":\"http://img.test/naver.png\"}}" +
                        "}", MediaType.APPLICATION_JSON));

        AuthResponse first = oauthLoginService.loginWithNaver("token-1");
        AuthResponse second = oauthLoginService.loginWithNaver("token-1");

        naverServer.verify();
        long count = userRepository.findAll().stream()
                .filter(u -> "naver".equals(u.getAuthProvider()))
                .count();
        assertThat(count).withFailMessage("네이버 사용자 저장 개수=%d", count).isEqualTo(1);
        assertThat(first.getUser().getId()).isEqualTo(second.getUser().getId());
        assertThat(second.getUser().getLocale()).isEqualTo("ko-KR");
    }
}
