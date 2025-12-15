package com.codexpong.backend.tournament.dto;

import com.codexpong.backend.tournament.domain.Tournament;
import com.codexpong.backend.tournament.domain.TournamentStatus;
import java.util.List;

/**
 * [응답] backend/src/main/java/com/codexpong/backend/tournament/dto/TournamentDetailResponse.java
 * 설명:
 *   - 참가자와 매치 리스트를 포함한 단일 토너먼트 상세 정보 응답이다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
public record TournamentDetailResponse(
        Long id,
        String name,
        Long creatorId,
        TournamentStatus status,
        Integer maxParticipants,
        List<TournamentParticipantResponse> participants,
        List<TournamentMatchResponse> matches
) {

    public static TournamentDetailResponse of(Tournament tournament, List<TournamentParticipantResponse> participants,
            List<TournamentMatchResponse> matches) {
        return new TournamentDetailResponse(tournament.getId(), tournament.getName(), tournament.getCreator().getId(),
                tournament.getStatus(), tournament.getMaxParticipants(), participants, matches);
    }
}
