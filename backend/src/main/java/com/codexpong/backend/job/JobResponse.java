package com.codexpong.backend.job;

import java.time.LocalDateTime;

/**
 * [응답 DTO] backend/src/main/java/com/codexpong/backend/job/JobResponse.java
 * 설명:
 *   - v0.12.0 잡 단건 조회 및 리스트 항목에 공통으로 사용되는 직렬화 모델이다.
 *   - 다운로드 링크는 API 엔드포인트 경로로 노출해 내부 파일 경로가 프런트에 노출되지 않도록 한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 */
public record JobResponse(Long jobId, JobType jobType, JobStatus status, int progress,
                          Long targetReplayId, LocalDateTime createdAt, LocalDateTime startedAt,
                          LocalDateTime endedAt, String errorCode, String errorMessage,
                          String resultUri, String downloadUrl) {

    public static JobResponse from(Job job) {
        String downloadUrl = job.getResultUri() != null ? "/api/jobs/" + job.getId() + "/result" : null;
        String exposedResult = job.getResultUri() != null ? downloadUrl : null;
        return new JobResponse(job.getId(), job.getJobType(), job.getStatus(), job.getProgress(),
                job.getTargetReplay().getId(), job.getCreatedAt(), job.getStartedAt(), job.getEndedAt(),
                job.getErrorCode(), job.getErrorMessage(), exposedResult, downloadUrl);
    }
}
