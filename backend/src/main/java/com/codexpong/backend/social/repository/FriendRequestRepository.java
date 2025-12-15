package com.codexpong.backend.social.repository;

import com.codexpong.backend.social.domain.FriendRequest;
import com.codexpong.backend.social.domain.FriendRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/social/repository/FriendRequestRepository.java
 * 설명:
 *   - 친구 요청의 생성/조회/상태 변경을 담당한다.
 *   - 송신자/수신자 조합별 최근 요청을 조회해 중복 요청을 방지한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findBySender_IdAndReceiver_IdAndStatus(Long senderId, Long receiverId,
            FriendRequestStatus status);

    Optional<FriendRequest> findByIdAndReceiver_Id(Long id, Long receiverId);

    List<FriendRequest> findByReceiver_IdOrderByCreatedAtDesc(Long receiverId);

    List<FriendRequest> findBySender_IdOrderByCreatedAtDesc(Long senderId);

    void deleteBySender_IdAndReceiver_Id(Long senderId, Long receiverId);
}
