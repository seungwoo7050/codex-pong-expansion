package com.codexpong.backend.tournament.repository;

import com.codexpong.backend.tournament.domain.Tournament;
import com.codexpong.backend.tournament.domain.TournamentMatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [리포지토리] backend/src/main/java/com/codexpong/backend/tournament/repository/TournamentMatchRepository.java
 * 설명:
 *   - 브래킷 매치 정보를 라운드/포지션/roomId 기준으로 조회한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {

    List<TournamentMatch> findByTournamentOrderByRoundNumberAscPositionAsc(Tournament tournament);

    Optional<TournamentMatch> findByTournamentAndRoundNumberAndPosition(Tournament tournament, Integer roundNumber,
            Integer position);

    Optional<TournamentMatch> findByRoomId(String roomId);
}
