package com.codexpong.backend.social.repository;

import com.codexpong.backend.social.domain.Friendship;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/social/repository/FriendshipRepository.java
 * 설명:
 *   - 친구 관계를 조회/저장하며 사용자 쌍 기준 중복 여부를 판단한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUserA_IdOrUserB_Id(Long userAId, Long userBId);

    Optional<Friendship> findByUserA_IdAndUserB_Id(Long userAId, Long userBId);
}
