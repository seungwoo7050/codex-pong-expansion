package com.codexpong.backend.social.dto;

import java.util.List;

/**
 * [응답 DTO] backend/src/main/java/com/codexpong/backend/social/dto/FriendRequestListResponse.java
 * 설명:
 *   - 수신/발신 친구 요청 목록을 한 번에 반환해 클라이언트 렌더링을 단순화한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
public record FriendRequestListResponse(List<FriendRequestResponse> incoming,
        List<FriendRequestResponse> outgoing) {
}
