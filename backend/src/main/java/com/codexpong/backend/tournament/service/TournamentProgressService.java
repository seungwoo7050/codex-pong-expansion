package com.codexpong.backend.tournament.service;

import com.codexpong.backend.game.GameResult;
import com.codexpong.backend.tournament.domain.Tournament;
import com.codexpong.backend.tournament.domain.TournamentMatch;
import com.codexpong.backend.tournament.domain.TournamentMatchStatus;
import com.codexpong.backend.tournament.domain.TournamentParticipant;
import com.codexpong.backend.tournament.repository.TournamentMatchRepository;
import com.codexpong.backend.tournament.repository.TournamentRepository;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/tournament/service/TournamentProgressService.java
 * 설명:
 *   - 게임 결과를 토너먼트 브래킷에 반영하고 다음 라운드 매치를 시작한다.
 *   - 최종 우승자가 결정되면 토너먼트 상태를 COMPLETED로 전환한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 *   - design/realtime/v0.7.0-tournament-events.md
 */
@Service
@Transactional
public class TournamentProgressService {

    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentService tournamentService;

    public TournamentProgressService(TournamentMatchRepository matchRepository,
            TournamentRepository tournamentRepository,
            TournamentService tournamentService) {
        this.matchRepository = matchRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentService = tournamentService;
    }

    @EventListener
    public void handleGameResult(GameResult result) {
        Optional<TournamentMatch> matchOpt = matchRepository.findByRoomId(result.getRoomId());
        if (matchOpt.isEmpty()) {
            return;
        }
        TournamentMatch match = matchOpt.get();
        if (match.getStatus() == TournamentMatchStatus.COMPLETED) {
            return;
        }
        TournamentParticipant winner = resolveWinner(match, result);
        match.complete(result.getScoreA(), result.getScoreB(), winner);
        matchRepository.save(match);

        propagateWinner(match, winner);
    }

    private TournamentParticipant resolveWinner(TournamentMatch match, GameResult result) {
        if (result.getScoreA() == result.getScoreB()) {
            return null;
        }
        Long winnerUserId = result.getScoreA() > result.getScoreB()
                ? result.getPlayerA().getId()
                : result.getPlayerB().getId();
        if (match.getParticipantA() != null && match.getParticipantA().getUser().getId().equals(winnerUserId)) {
            return match.getParticipantA();
        }
        if (match.getParticipantB() != null && match.getParticipantB().getUser().getId().equals(winnerUserId)) {
            return match.getParticipantB();
        }
        return null;
    }

    private void propagateWinner(TournamentMatch match, TournamentParticipant winner) {
        Tournament tournament = match.getTournament();
        int nextRound = match.getRoundNumber() + 1;
        int nextPosition = match.getPosition() / 2;
        Optional<TournamentMatch> nextMatchOpt = matchRepository.findByTournamentAndRoundNumberAndPosition(tournament,
                nextRound, nextPosition);
        if (nextMatchOpt.isEmpty()) {
            completeTournament(tournament);
            return;
        }
        TournamentMatch nextMatch = nextMatchOpt.get();
        if (match.getPosition() % 2 == 0) {
            nextMatch.assignParticipants(winner, nextMatch.getParticipantB());
        } else {
            nextMatch.assignParticipants(nextMatch.getParticipantA(), winner);
        }
        matchRepository.save(nextMatch);
        if (nextMatch.readyToStart()) {
            tournamentService.openMatch(nextMatch);
        }
        tournamentService.publishUpdatedDetail(tournament, "TOURNAMENT_UPDATED");
    }

    private void completeTournament(Tournament tournament) {
        tournament.markCompleted();
        tournamentRepository.save(tournament);
        tournamentService.publishUpdatedDetail(tournament, "TOURNAMENT_COMPLETED");
    }
}
