package com.codexpong.backend.tournament.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * [요청] backend/src/main/java/com/codexpong/backend/tournament/dto/TournamentCreateRequest.java
 * 설명:
 *   - 토너먼트 생성 시 이름과 최대 참가 인원을 전달한다.
 *   - 단일 제거 간단 규칙을 위해 4~16명 범위를 유효성 검사한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
public record TournamentCreateRequest(
        @NotBlank String name,
        @Min(4) @Max(16) Integer maxParticipants
) {
}
