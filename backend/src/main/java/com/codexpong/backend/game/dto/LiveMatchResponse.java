package com.codexpong.backend.game.dto;

import com.codexpong.backend.common.KstDateTime;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.game.service.GameRoomService.LiveRoomView;
import java.time.OffsetDateTime;

/**
 * [응답] backend/src/main/java/com/codexpong/backend/game/dto/LiveMatchResponse.java
 * 설명:
 *   - 관전 가능한 진행 중 경기 정보를 노출하기 위한 응답 모델이다.
 *   - 좌/우 플레이어 닉네임, 관전자 수/제한, 시작 시각을 포함한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 */
public record LiveMatchResponse(
        String roomId,
        MatchType matchType,
        Long leftPlayerId,
        String leftNickname,
        Long rightPlayerId,
        String rightNickname,
        OffsetDateTime startedAt,
        int spectatorCount,
        int spectatorLimit
) {

    public static LiveMatchResponse from(LiveRoomView view) {
        return new LiveMatchResponse(
                view.roomId(),
                view.matchType(),
                view.leftPlayerId(),
                view.leftNickname(),
                view.rightPlayerId(),
                view.rightNickname(),
                KstDateTime.toOffset(view.startedAt()),
                view.spectatorCount(),
                view.spectatorLimit()
        );
    }
}
