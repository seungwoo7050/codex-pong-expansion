package com.codexpong.backend.social.dto;

import com.codexpong.backend.common.KstDateTime;
import com.codexpong.backend.user.domain.User;
import java.time.OffsetDateTime;

/**
 * [응답 DTO] backend/src/main/java/com/codexpong/backend/social/dto/FriendSummaryResponse.java
 * 설명:
 *   - 친구 목록에서 표기할 핵심 정보(닉네임, 아바타, 온라인 여부, 친구가 된 시점)를 반환한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/backend/v0.10.0-kor-auth-and-locale.md
 */
public record FriendSummaryResponse(Long userId, String nickname, String avatarUrl, boolean online,
        OffsetDateTime since) {

    public static FriendSummaryResponse from(User friend, boolean online, java.time.LocalDateTime since) {
        return new FriendSummaryResponse(friend.getId(), friend.getNickname(), friend.getAvatarUrl(), online,
                KstDateTime.toOffset(since));
    }
}
