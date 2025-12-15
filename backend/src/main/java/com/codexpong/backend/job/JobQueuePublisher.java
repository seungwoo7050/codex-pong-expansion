package com.codexpong.backend.job;

import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * [디스패처] backend/src/main/java/com/codexpong/backend/job/JobQueuePublisher.java
 * 설명:
 *   - v0.12.0 Redis Streams 기반 워커 큐로 잡 요청 메시지를 발행한다.
 *   - 동일 jobId는 한 번만 발행되도록 서비스 레이어에서 선행 검증을 수행하며, 여기서는 단순히 스트림에 적재한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/backend/v0.12.0-jobs-api-and-state-machine.md
 *   - design/infra/v0.12.0-worker-and-queue-topology.md
 */
@Component
public class JobQueuePublisher {

    private final StringRedisTemplate redisTemplate;
    private final JobQueueProperties properties;

    public JobQueuePublisher(StringRedisTemplate redisTemplate, JobQueueProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void publish(JobRequestMessage message) {
        if (!properties.isEnabled()) {
            return;
        }
        redisTemplate.opsForStream().add(StreamRecords.mapBacked(message.toMap())
                .withStreamKey(properties.getRequestStream())
                .withId(RecordId.autoGenerate()));
    }
}
