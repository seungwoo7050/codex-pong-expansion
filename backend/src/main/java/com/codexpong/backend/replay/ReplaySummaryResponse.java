package com.codexpong.backend.replay;

import com.codexpong.backend.common.KstDateTime;
import com.codexpong.backend.game.GameResult;
import com.codexpong.backend.user.domain.User;
import java.time.OffsetDateTime;

/**
 * [DTO] backend/src/main/java/com/codexpong/backend/replay/ReplaySummaryResponse.java
 * 설명:
 *   - 리플레이 목록/상세 공통으로 사용하는 요약 정보를 노출한다.
 *   - 소유자 관점에서 상대 닉네임과 점수를 계산한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/backend/v0.11.0-replay-recording-and-storage.md
 */
public record ReplaySummaryResponse(
        Long replayId,
        Long matchId,
        Long ownerUserId,
        Long opponentUserId,
        String opponentNickname,
        String matchType,
        int myScore,
        int opponentScore,
        long durationMs,
        OffsetDateTime createdAt,
        String eventFormat
) {

    public static ReplaySummaryResponse from(Replay replay, User owner) {
        GameResult match = replay.getMatch();
        boolean ownerIsLeft = match.getPlayerA().getId().equals(owner.getId());
        User opponent = ownerIsLeft ? match.getPlayerB() : match.getPlayerA();
        int myScore = ownerIsLeft ? match.getScoreA() : match.getScoreB();
        int opponentScore = ownerIsLeft ? match.getScoreB() : match.getScoreA();
        return new ReplaySummaryResponse(
                replay.getId(),
                match.getId(),
                owner.getId(),
                opponent.getId(),
                opponent.getNickname(),
                match.getMatchType(),
                myScore,
                opponentScore,
                replay.getDurationMs(),
                KstDateTime.toOffset(replay.getCreatedAt()),
                replay.getEventFormat()
        );
    }
}
