package com.codexpong.backend.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * [DTO] backend/src/main/java/com/codexpong/backend/game/GameResultRequest.java
 * 설명:
 *   - 테스트 경기 결과 생성 시 클라이언트가 전달하는 필드를 검증한다.
 * 버전: v0.1.0
 * 관련 설계문서:
 *   - design/backend/v0.1.0-core-skeleton-and-health.md
 * 변경 이력:
 *   - v0.1.0: 필드 검증 추가
 */
public record GameResultRequest(
        @NotBlank(message = "playerA는 필수입니다")
        @Size(max = 50, message = "playerA는 50자 이하여야 합니다")
        String playerA,

        @NotBlank(message = "playerB는 필수입니다")
        @Size(max = 50, message = "playerB는 50자 이하여야 합니다")
        String playerB,

        @Min(value = 0, message = "scoreA는 0 이상이어야 합니다")
        @Max(value = 30, message = "scoreA는 30 이하로 제한합니다")
        int scoreA,

        @Min(value = 0, message = "scoreB는 0 이상이어야 합니다")
        @Max(value = 30, message = "scoreB는 30 이하로 제한합니다")
        int scoreB
) {
}
