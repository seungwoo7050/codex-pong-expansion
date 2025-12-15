package com.codexpong.backend.job;

import java.util.Map;

/**
 * [메시지] backend/src/main/java/com/codexpong/backend/job/JobProgressMessage.java
 * 설명:
 *   - 워커가 전송하는 진행률 이벤트를 표현하며, Redis Streams payload를 객체로 변환한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/realtime/v0.12.0-job-progress-events.md
 */
public record JobProgressMessage(Long jobId, int progress, String phase, String message) {

    public static JobProgressMessage fromRaw(Map<String, String> map) {
        Long jobId = Long.parseLong(map.getOrDefault("jobId", "0"));
        int progress = Integer.parseInt(map.getOrDefault("progress", "0"));
        String phase = map.getOrDefault("phase", "");
        String message = map.getOrDefault("message", "");
        return new JobProgressMessage(jobId, progress, phase, message);
    }
}
