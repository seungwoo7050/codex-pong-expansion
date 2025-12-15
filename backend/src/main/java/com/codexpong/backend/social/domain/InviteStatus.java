package com.codexpong.backend.social.domain;

/**
 * [열거형] backend/src/main/java/com/codexpong/backend/social/domain/InviteStatus.java
 * 설명:
 *   - 게임 초대의 진행 상태를 표현한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
public enum InviteStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
