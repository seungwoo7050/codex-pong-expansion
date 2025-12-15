package com.codexpong.backend.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.codexpong.backend.auth.dto.AuthResponse;
import com.codexpong.backend.auth.dto.LoginRequest;
import com.codexpong.backend.auth.dto.RegisterRequest;
import com.codexpong.backend.auth.service.AuthService;
import com.codexpong.backend.auth.service.AuthTokenService;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * [테스트] 회원 가입 및 로그인 로직을 검증한다.
 * 설명:
 *   - v0.2.0 인증 흐름에서 토큰 발급과 비밀번호 검증이 제대로 동작하는지 확인한다.
 *   - v0.4.0에서 추가된 레이팅 필드가 응답에 포함되는지 점검한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 시 비밀번호 암호화와 토큰 발급이 수행된다")
    void registerShouldEncryptPasswordAndReturnToken() {
        RegisterRequest request = new RegisterRequest(
                "unit-register",
                "password123",
                "테스터",
                "https://example.com/avatar.png"
        );

        AuthResponse response = authService.register(request);

        User saved = userRepository.findByUsername("unit-register").orElseThrow();
        assertNotEquals("password123", saved.getPassword(), "평문 비밀번호가 저장되면 안 된다");
        assertTrue(passwordEncoder.matches("password123", saved.getPassword()), "암호화된 비밀번호가 저장되어야 한다");
        assertNotNull(response.getUser(), "회원 정보 응답이 포함되어야 한다");
        assertEquals(saved.getRating(), response.getUser().getRating(), "기본 레이팅이 응답에 노출되어야 한다");
        assertTrue(authTokenService.parse(response.getToken()).isPresent(), "발급된 토큰을 파싱할 수 있어야 한다");
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인하면 401 예외를 반환한다")
    void loginWithWrongPasswordShouldFail() {
        authService.register(new RegisterRequest("unit-login", "password123", "테스터2", null));

        LoginRequest wrongPassword = new LoginRequest("unit-login", "wrong-pass");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(wrongPassword));
        assertEquals(401, exception.getStatusCode().value(), "401 상태가 반환되어야 한다");
    }

    @Test
    @DisplayName("중복 아이디로 회원가입을 시도하면 409 예외가 발생한다")
    void duplicateUsernameShouldReturnConflict() {
        authService.register(new RegisterRequest("unit-duplicate", "password123", "테스터3", null));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(new RegisterRequest("unit-duplicate", "password123", "테스터3", null)));
        assertEquals(409, exception.getStatusCode().value(), "중복 아이디는 409 상태로 거부되어야 한다");
    }
}
