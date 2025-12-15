package com.codexpong.backend.social.dto;

import jakarta.validation.constraints.NotNull;

/**
 * [요청 DTO] backend/src/main/java/com/codexpong/backend/social/dto/InviteSendRequest.java
 * 설명:
 *   - 친구에게 게임 초대를 보낼 때 대상 사용자 id를 전달한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
public class InviteSendRequest {

    @NotNull
    private Long targetUserId;

    public Long getTargetUserId() {
        return targetUserId;
    }
}
