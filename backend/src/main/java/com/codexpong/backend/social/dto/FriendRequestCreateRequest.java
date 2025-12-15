package com.codexpong.backend.social.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [요청 DTO] backend/src/main/java/com/codexpong/backend/social/dto/FriendRequestCreateRequest.java
 * 설명:
 *   - 사용자명이 주어졌을 때 친구 요청을 생성한다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
public class FriendRequestCreateRequest {

    @NotBlank
    private String targetUsername;

    public String getTargetUsername() {
        return targetUsername;
    }
}
