package com.codexpong.backend.job;

import java.util.HashMap;
import java.util.Map;

/**
 * [메시지] backend/src/main/java/com/codexpong/backend/job/JobRequestMessage.java
 * 설명:
 *   - Redis Streams에 게시되는 내보내기 요청 payload 스키마를 정의한다.
 *   - 필수 필드 외 옵션은 문자열 맵으로 유지해 워커 확장성을 확보한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/infra/v0.12.0-worker-and-queue-topology.md
 */
public record JobRequestMessage(Long jobId, JobType jobType, Long replayId, Map<String, String> options) {

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("jobId", String.valueOf(jobId));
        map.put("jobType", jobType.name());
        map.put("replayId", String.valueOf(replayId));
        options.forEach(map::put);
        return map;
    }
}
