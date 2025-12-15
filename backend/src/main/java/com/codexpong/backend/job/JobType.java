package com.codexpong.backend.job;

/**
 * [열거형] backend/src/main/java/com/codexpong/backend/job/JobType.java
 * 설명:
 *   - v0.12.0 리플레이 내보내기 잡 유형을 정의한다.
 *   - 워커와 API 계약 모두에서 동일한 문자열을 사용한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 */
public enum JobType {
    REPLAY_EXPORT_MP4,
    REPLAY_THUMBNAIL
}
