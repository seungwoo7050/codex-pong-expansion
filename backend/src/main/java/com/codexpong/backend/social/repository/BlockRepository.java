package com.codexpong.backend.social.repository;

import com.codexpong.backend.social.domain.Block;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/social/repository/BlockRepository.java
 * 설명:
 *   - 차단 관계를 저장하고 특정 사용자 간 차단 여부를 빠르게 조회한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
public interface BlockRepository extends JpaRepository<Block, Long> {

    boolean existsByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    List<Block> findByBlocker_Id(Long blockerId);

    Optional<Block> findByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);
}
