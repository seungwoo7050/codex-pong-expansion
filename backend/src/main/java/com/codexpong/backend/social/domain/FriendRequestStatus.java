package com.codexpong.backend.social.domain;

/**
 * [열거형] backend/src/main/java/com/codexpong/backend/social/domain/FriendRequestStatus.java
 * 설명:
 *   - 친구 요청이 현재 어떤 상태인지 표현한다.
 *   - v0.5.0 친구/차단/초대 흐름에서 요청 수락/거절 결과를 명확히 기록한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
public enum FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
