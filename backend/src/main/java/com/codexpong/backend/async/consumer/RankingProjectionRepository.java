package com.codexpong.backend.async.consumer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/async/consumer/RankingProjectionRepository.java
 * 설명:
 *   - 레이팅 프로젝션을 조회/업데이트한다.
 */
public interface RankingProjectionRepository extends JpaRepository<RankingProjection, Long> {

    Optional<RankingProjection> findByUserId(Long userId);
}
