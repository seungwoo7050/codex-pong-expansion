package com.codexpong.backend.social.repository;

import com.codexpong.backend.social.domain.GameInvite;
import com.codexpong.backend.social.domain.InviteStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/social/repository/GameInviteRepository.java
 * 설명:
 *   - 게임 초대 요청을 조회/저장하고 대기 중 초대를 빠르게 찾는다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
public interface GameInviteRepository extends JpaRepository<GameInvite, Long> {

    List<GameInvite> findByReceiver_IdAndStatusOrderByCreatedAtDesc(Long receiverId, InviteStatus status);

    List<GameInvite> findBySender_IdAndStatusOrderByCreatedAtDesc(Long senderId, InviteStatus status);

    Optional<GameInvite> findByIdAndReceiver_Id(Long id, Long receiverId);

    Optional<GameInvite> findBySender_IdAndReceiver_IdAndStatus(Long senderId, Long receiverId, InviteStatus status);
}
