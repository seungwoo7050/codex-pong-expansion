package com.codexpong.backend.tournament.repository;

import com.codexpong.backend.tournament.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/tournament/repository/TournamentRepository.java
 * 설명:
 *   - 토너먼트 엔티티를 조회/저장하기 위한 JPA 리포지토리다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
}
