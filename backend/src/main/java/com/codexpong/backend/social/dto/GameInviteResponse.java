package com.codexpong.backend.social.dto;

import com.codexpong.backend.common.KstDateTime;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.social.domain.GameInvite;
import com.codexpong.backend.social.domain.InviteStatus;
import java.time.OffsetDateTime;

/**
 * [응답 DTO] backend/src/main/java/com/codexpong/backend/social/dto/GameInviteResponse.java
 * 설명:
 *   - 게임 초대 상태를 반환해 초대 목록 및 수락 결과를 클라이언트에서 표시한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 */
public record GameInviteResponse(Long id, Long senderId, String senderNickname, Long receiverId, InviteStatus status,
        MatchType matchType, String roomId, OffsetDateTime createdAt, OffsetDateTime respondedAt) {

    public static GameInviteResponse from(GameInvite invite) {
        return new GameInviteResponse(
                invite.getId(),
                invite.getSender().getId(),
                invite.getSender().getNickname(),
                invite.getReceiver().getId(),
                invite.getStatus(),
                invite.getMatchType(),
                invite.getRoomId(),
                KstDateTime.toOffset(invite.getCreatedAt()),
                KstDateTime.toOffset(invite.getRespondedAt())
        );
    }
}
