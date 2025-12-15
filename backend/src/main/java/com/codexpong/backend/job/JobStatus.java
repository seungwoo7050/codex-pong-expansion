package com.codexpong.backend.job;

/**
 * [열거형] backend/src/main/java/com/codexpong/backend/job/JobStatus.java
 * 설명:
 *   - v0.12.0 리플레이 내보내기 잡의 상태를 문자열로 관리한다.
 *   - WebSocket 및 REST 응답에서 동일한 토큰을 사용해 일관성을 유지한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 */
public enum JobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
