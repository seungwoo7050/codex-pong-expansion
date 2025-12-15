package com.codexpong.backend.tournament.dto;

import com.codexpong.backend.tournament.domain.Tournament;
import com.codexpong.backend.tournament.domain.TournamentStatus;

/**
 * [응답] backend/src/main/java/com/codexpong/backend/tournament/dto/TournamentSummaryResponse.java
 * 설명:
 *   - 토너먼트 목록 조회 시 사용되는 요약 응답 모델이다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
public record TournamentSummaryResponse(
        Long id,
        String name,
        Long creatorId,
        TournamentStatus status,
        Integer maxParticipants,
        Integer currentParticipants
) {

    public static TournamentSummaryResponse of(Tournament tournament, int currentParticipants) {
        return new TournamentSummaryResponse(tournament.getId(), tournament.getName(), tournament.getCreator().getId(),
                tournament.getStatus(), tournament.getMaxParticipants(), currentParticipants);
    }
}
