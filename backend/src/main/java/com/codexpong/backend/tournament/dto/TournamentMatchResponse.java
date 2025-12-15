package com.codexpong.backend.tournament.dto;

import com.codexpong.backend.tournament.domain.TournamentMatch;
import com.codexpong.backend.tournament.domain.TournamentMatchStatus;

/**
 * [응답] backend/src/main/java/com/codexpong/backend/tournament/dto/TournamentMatchResponse.java
 * 설명:
 *   - 라운드, 포지션, 참가자/승자 정보를 포함한 매치 상태 DTO다.
 *   - roomId를 포함해 게임 입장에 활용한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
public record TournamentMatchResponse(
        Long id,
        Integer round,
        Integer position,
        TournamentMatchStatus status,
        String roomId,
        Integer scoreA,
        Integer scoreB,
        TournamentParticipantResponse participantA,
        TournamentParticipantResponse participantB,
        Long winnerId
) {

    public static TournamentMatchResponse from(TournamentMatch match) {
        return new TournamentMatchResponse(
                match.getId(),
                match.getRoundNumber(),
                match.getPosition(),
                match.getStatus(),
                match.getRoomId(),
                match.getScoreA(),
                match.getScoreB(),
                match.getParticipantA() != null ? TournamentParticipantResponse.from(match.getParticipantA()) : null,
                match.getParticipantB() != null ? TournamentParticipantResponse.from(match.getParticipantB()) : null,
                match.getWinner() != null ? match.getWinner().getUser().getId() : null
        );
    }
}
