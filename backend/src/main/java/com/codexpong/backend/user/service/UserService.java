package com.codexpong.backend.user.service;

import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.dto.ProfileUpdateRequest;
import com.codexpong.backend.user.dto.UserResponse;
import com.codexpong.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/user/service/UserService.java
 * 설명:
 *   - 로그인한 사용자의 프로필 조회 및 수정 로직을 담당한다.
 *   - 존재하지 않는 사용자의 접근을 방지하고 입력값을 엔티티에 반영한다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 * 변경 이력:
 *   - v0.2.0: 프로필 조회/수정 서비스 추가
 *   - v0.4.0: 레이팅 필드 반환 및 랭킹 연계 대비
 *   - v0.9.0: 밴/정지 검증 헬퍼 추가
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    public UserResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        user.updateProfile(request.getNickname(), request.getAvatarUrl());
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    /**
     * 설명:
     *   - 게임/매칭 등 내부 도메인 로직에서 사용할 엔티티를 조회한다.
     */
    public User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    /**
     * 설명:
     *   - 게임/매칭 등의 핵심 흐름에서 밴/정지 상태를 검증한 뒤 사용자 엔티티를 반환한다.
     */
    public User getActiveUserEntity(Long userId) {
        User user = getUserEntity(userId);
        if (user.isBanned()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "밴된 계정은 이용할 수 없습니다.");
        }
        if (user.isSuspended()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지 기간이 끝난 후 다시 시도하세요.");
        }
        return user;
    }
}
