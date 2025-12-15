package com.codexpong.backend.async.consumer;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/async/consumer/AbuseSignalRepository.java
 * 설명:
 *   - 이상 징후 기록을 조회한다.
 */
public interface AbuseSignalRepository extends JpaRepository<AbuseSignal, Long> {
}
