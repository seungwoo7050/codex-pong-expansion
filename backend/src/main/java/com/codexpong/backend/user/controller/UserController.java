package com.codexpong.backend.user.controller;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.user.dto.ProfileUpdateRequest;
import com.codexpong.backend.user.dto.UserResponse;
import com.codexpong.backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/user/controller/UserController.java
 * 설명:
 *   - 로그인 사용자의 기본 프로필 조회와 수정 엔드포인트를 제공한다.
 *   - 인증 정보는 SecurityContext에 적재된 AuthenticatedUser에서 가져온다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/backend/v0.2.0-auth-and-profile.md
 * 변경 이력:
 *   - v0.2.0: 내 프로필 조회/수정 API 추가
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 설명:
     *   - 인증된 사용자의 프로필을 반환한다.
     * 출력:
     *   - UserResponse: 아이디, 닉네임, 아바타, 생성/수정 시각
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return userService.getProfile(user.id());
    }

    /**
     * 설명:
     *   - 닉네임과 아바타 URL을 수정하고 최신 프로필을 반환한다.
     * 입력:
     *   - ProfileUpdateRequest: nickname, avatarUrl
     * 출력:
     *   - UserResponse: 수정 결과 반영된 프로필
     */
    @PutMapping("/me")
    public UserResponse update(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return userService.updateProfile(user.id(), request);
    }
}
