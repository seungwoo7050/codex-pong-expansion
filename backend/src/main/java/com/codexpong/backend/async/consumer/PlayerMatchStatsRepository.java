package com.codexpong.backend.async.consumer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/async/consumer/PlayerMatchStatsRepository.java
 * 설명:
 *   - 사용자별 통계 프로젝션을 조회/업데이트한다.
 */
public interface PlayerMatchStatsRepository extends JpaRepository<PlayerMatchStats, Long> {

    Optional<PlayerMatchStats> findByUserId(Long userId);
}
