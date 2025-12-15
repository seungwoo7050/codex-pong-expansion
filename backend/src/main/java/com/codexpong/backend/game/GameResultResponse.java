package com.codexpong.backend.game;

import com.codexpong.backend.common.KstDateTime;
import java.time.OffsetDateTime;

/**
 * [DTO] backend/src/main/java/com/codexpong/backend/game/GameResultResponse.java
 * 설명:
 *   - 클라이언트에 노출할 경기 결과 정보를 단순화한 응답 모델이다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 * 변경 이력:
 *   - v0.1.0: 엔티티 매핑 전용 DTO 추가
 *   - v0.3.0: 사용자/시간/룸 정보를 포함하도록 확장
 *   - v0.4.0: 매치 타입과 레이팅 변동 정보를 포함
 *   - v0.10.0: 타임스탬프를 KST OffsetDateTime으로 변환
 */
public record GameResultResponse(
        Long id,
        Long playerAId,
        String playerANickname,
        Long playerBId,
        String playerBNickname,
        int scoreA,
        int scoreB,
        String matchType,
        int ratingChangeA,
        int ratingChangeB,
        int ratingAfterA,
        int ratingAfterB,
        String roomId,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt
) {

    public static GameResultResponse from(GameResult entity) {
        return new GameResultResponse(
                entity.getId(),
                entity.getPlayerA().getId(),
                entity.getPlayerA().getNickname(),
                entity.getPlayerB().getId(),
                entity.getPlayerB().getNickname(),
                entity.getScoreA(),
                entity.getScoreB(),
                entity.getMatchType(),
                entity.getRatingChangeA(),
                entity.getRatingChangeB(),
                entity.getRatingAfterA(),
                entity.getRatingAfterB(),
                entity.getRoomId(),
                KstDateTime.toOffset(entity.getStartedAt()),
                KstDateTime.toOffset(entity.getFinishedAt())
        );
    }
}
