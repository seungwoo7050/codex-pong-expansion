package com.codexpong.backend.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * [요청 DTO] backend/src/main/java/com/codexpong/backend/admin/dto/ModerationRequest.java
 * 설명:
 *   - 관리자 API에서 밴/정지/뮤트를 적용할 때 전달되는 요청 모델이다.
 *   - durationMinutes가 0 이하일 경우 즉시 적용/해제 용도로 활용한다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/backend/v0.9.0-admin-and-ops.md
 */
public class ModerationRequest {

    @NotNull
    private ActionType action;

    @Min(0)
    private int durationMinutes;

    @NotBlank
    private String reason;

    public ActionType getAction() {
        return action;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getReason() {
        return reason;
    }

    public enum ActionType {
        BAN,
        SUSPEND,
        MUTE
    }
}
