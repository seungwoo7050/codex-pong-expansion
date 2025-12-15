package com.codexpong.backend.replay;

import com.codexpong.backend.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * [저장소] backend/src/main/java/com/codexpong/backend/replay/ReplayRepository.java
 * 설명:
 *   - 리플레이 메타데이터를 조회/저장하기 위한 JPA 리포지토리다.
 *   - 소유자별 조회와 스토리지 URI 기반 참조 수 계산을 제공한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/backend/v0.11.0-replay-recording-and-storage.md
 */
@Repository
public interface ReplayRepository extends JpaRepository<Replay, Long> {

    Page<Replay> findByOwnerOrderByCreatedAtDesc(User owner, Pageable pageable);

    Optional<Replay> findByIdAndOwner(Long id, User owner);

    Optional<Replay> findByMatchIdAndOwner(Long matchId, User owner);

    List<Replay> findByOwnerOrderByCreatedAtDesc(User owner);

    long countByStorageUri(String storageUri);
}
