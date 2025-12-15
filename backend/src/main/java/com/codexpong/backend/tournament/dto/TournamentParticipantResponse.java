package com.codexpong.backend.tournament.dto;

import com.codexpong.backend.tournament.domain.TournamentParticipant;

/**
 * [응답] backend/src/main/java/com/codexpong/backend/tournament/dto/TournamentParticipantResponse.java
 * 설명:
 *   - 토너먼트 참가자 정보를 프런트엔드로 전달하기 위한 DTO.
 *   - 사용자 닉네임과 시드 순서를 포함한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
public record TournamentParticipantResponse(
        Long id,
        Long userId,
        String nickname,
        Integer seed
) {

    public static TournamentParticipantResponse from(TournamentParticipant participant) {
        return new TournamentParticipantResponse(participant.getId(), participant.getUser().getId(),
                participant.getUser().getNickname(), participant.getSeed());
    }
}
