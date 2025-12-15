package com.codexpong.backend.job;

/**
 * [응답 DTO] backend/src/main/java/com/codexpong/backend/job/JobCreateResponse.java
 * 설명:
 *   - v0.12.0 내보내기 잡 생성 시 즉시 반환되는 jobId를 감싼다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 */
public record JobCreateResponse(Long jobId) {
}
