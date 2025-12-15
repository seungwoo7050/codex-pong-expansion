package com.codexpong.backend.user;

import static org.junit.jupiter.api.Assertions.*;

import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.dto.ProfileUpdateRequest;
import com.codexpong.backend.user.dto.UserResponse;
import com.codexpong.backend.user.repository.UserRepository;
import com.codexpong.backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * [테스트] 프로필 조회 및 수정 흐름을 검증한다.
 * 설명:
 *   - v0.2.0 프로필 서비스가 존재하지 않는 사용자를 적절히 처리하는지 확인한다.
 *   - v0.4.0에서 레이팅 필드가 유지되는지 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("프로필 조회 시 저장된 닉네임과 아바타, 레이팅을 반환한다")
    void getProfileReturnsPersistedFields() {
        User user = new User("member1", "encoded-password", "회원", "avatar-1.png");
        user.updateRating(1320);
        User saved = userRepository.save(user);

        UserResponse response = userService.getProfile(saved.getId());

        assertEquals("member1", response.getUsername(), "아이디가 일치해야 한다");
        assertEquals("회원", response.getNickname(), "닉네임을 그대로 반환해야 한다");
        assertEquals("avatar-1.png", response.getAvatarUrl(), "아바타 URL이 반영되어야 한다");
        assertEquals(1320, response.getRating(), "변경된 레이팅이 유지되어야 한다");
    }

    @Test
    @DisplayName("프로필 수정 시 닉네임과 아바타가 갱신된다")
    void updateProfileUpdatesNicknameAndAvatar() {
        User user = new User("member2", "encoded-password", "초기닉", "avatar-old.png");
        User saved = userRepository.save(user);

        ProfileUpdateRequest request = new ProfileUpdateRequest("새닉네임", "avatar-new.png");
        UserResponse response = userService.updateProfile(saved.getId(), request);

        assertEquals("새닉네임", response.getNickname(), "닉네임이 수정되어야 한다");
        assertEquals("avatar-new.png", response.getAvatarUrl(), "아바타 URL이 수정되어야 한다");
        User refreshed = userRepository.findById(saved.getId()).orElseThrow();
        assertEquals("새닉네임", refreshed.getNickname(), "저장소에도 변경 사항이 반영되어야 한다");
        assertEquals("avatar-new.png", refreshed.getAvatarUrl(), "저장소에도 새 아바타가 저장되어야 한다");
    }
}
