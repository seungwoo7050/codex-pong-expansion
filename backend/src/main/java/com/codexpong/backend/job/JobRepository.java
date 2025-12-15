package com.codexpong.backend.job;

import com.codexpong.backend.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [저장소] backend/src/main/java/com/codexpong/backend/job/JobRepository.java
 * 설명:
 *   - v0.12.0 잡 엔티티의 기본 CRUD와 소유자 기반 조회 쿼리를 제공한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 */
public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByIdAndOwner(Long id, User owner);

    Page<Job> findByOwner(User owner, Pageable pageable);

    Page<Job> findByOwnerAndJobType(User owner, JobType jobType, Pageable pageable);

    Page<Job> findByOwnerAndStatus(User owner, JobStatus status, Pageable pageable);

    Page<Job> findByOwnerAndJobTypeAndStatus(User owner, JobType jobType, JobStatus status, Pageable pageable);

    List<Job> findByOwner(User owner);
}
