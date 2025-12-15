package com.codexpong.backend.tournament.repository;

import com.codexpong.backend.tournament.domain.Tournament;
import com.codexpong.backend.tournament.domain.TournamentParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/tournament/repository/TournamentParticipantRepository.java
 * 설명:
 *   - 토너먼트 참가자 목록을 조회하고 사용자 중복 참여 여부를 검사한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {

    List<TournamentParticipant> findByTournamentOrderBySeedAsc(Tournament tournament);

    boolean existsByTournament_IdAndUser_Id(Long tournamentId, Long userId);

    long countByTournament_Id(Long tournamentId);

    Optional<TournamentParticipant> findByTournament_IdAndUser_Id(Long tournamentId, Long userId);
}
