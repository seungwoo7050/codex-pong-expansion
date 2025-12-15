package com.codexpong.backend.async.event;

import com.codexpong.backend.game.GameResult;
import java.time.LocalDateTime;

/**
 * [레코드] backend/src/main/java/com/codexpong/backend/async/event/MatchResultEventPayload.java
 * 설명:
 *   - 경기 종료 시 발행되는 이벤트 페이로드를 직렬화한다.
 *   - 이벤트 기반 소비자들이 동일한 정보를 사용하도록 구조를 고정한다.
 */
public record MatchResultEventPayload(
        String eventId,
        Long gameResultId,
        String roomId,
        Long playerAId,
        Long playerBId,
        int scoreA,
        int scoreB,
        String matchType,
        int ratingChangeA,
        int ratingChangeB,
        int ratingAfterA,
        int ratingAfterB,
        LocalDateTime startedAt,
        LocalDateTime finishedAt) {

    public static MatchResultEventPayload from(String eventId, GameResult result) {
        return new MatchResultEventPayload(
                eventId,
                result.getId(),
                result.getRoomId(),
                result.getPlayerA().getId(),
                result.getPlayerB().getId(),
                result.getScoreA(),
                result.getScoreB(),
                result.getMatchType(),
                result.getRatingChangeA(),
                result.getRatingChangeB(),
                result.getRatingAfterA(),
                result.getRatingAfterB(),
                result.getStartedAt(),
                result.getFinishedAt()
        );
    }
}
