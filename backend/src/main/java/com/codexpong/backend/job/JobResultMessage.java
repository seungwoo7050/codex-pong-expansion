package com.codexpong.backend.job;

import java.util.Map;

/**
 * [메시지] backend/src/main/java/com/codexpong/backend/job/JobResultMessage.java
 * 설명:
 *   - 워커가 완료/실패 결과를 게시할 때 사용하는 Redis Streams payload 스키마를 표현한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/infra/v0.12.0-worker-and-queue-topology.md
 */
public record JobResultMessage(Long jobId, JobStatus status, String resultUri, String checksum,
                               String errorCode, String errorMessage) {

    public static JobResultMessage fromRaw(Map<String, String> map) {
        Long jobId = Long.parseLong(map.getOrDefault("jobId", "0"));
        JobStatus status = JobStatus.valueOf(map.getOrDefault("status", JobStatus.FAILED.name()));
        String resultUri = map.getOrDefault("resultUri", "");
        String checksum = map.getOrDefault("checksum", "");
        String errorCode = map.getOrDefault("errorCode", "");
        String errorMessage = map.getOrDefault("errorMessage", "");
        return new JobResultMessage(jobId, status, resultUri, checksum, errorCode, errorMessage);
    }
}
