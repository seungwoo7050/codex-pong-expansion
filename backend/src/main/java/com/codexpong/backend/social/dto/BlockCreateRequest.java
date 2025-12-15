package com.codexpong.backend.social.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [요청 DTO] backend/src/main/java/com/codexpong/backend/social/dto/BlockCreateRequest.java
 * 설명:
 *   - 지정된 사용자명을 차단 목록에 추가하기 위한 입력이다.
 * 버전: v0.5.0
 * 관련 설계문서:
 *   - design/backend/v0.5.0-friends-and-blocks.md
 */
public class BlockCreateRequest {

    @NotBlank
    private String targetUsername;

    public String getTargetUsername() {
        return targetUsername;
    }
}
