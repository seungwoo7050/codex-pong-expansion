package com.codexpong.backend.user.repository;

import com.codexpong.backend.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/user/repository/UserRepository.java
 * 설명:
 *   - 사용자 엔티티에 대한 기본 CRUD 및 조회 기능을 제공한다.
 *   - 로그인 아이디 중복 검사와 인증 시 사용자 조회에 사용된다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/backend/v0.4.0-ranking-system.md
 * 변경 이력:
 *   - v0.2.0: 사용자 리포지토리 인터페이스 추가
 *   - v0.4.0: 레이팅 순위 조회 쿼리 추가
 *   - v0.10.0: OAuth 공급자 조회 메서드 추가
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByAuthProviderAndProviderUserId(String authProvider, String providerUserId);

    List<User> findTop20ByOrderByRatingDesc();
}
