package com.codexpong.backend.job;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * [리스너] backend/src/main/java/com/codexpong/backend/job/JobQueueListener.java
 * 설명:
 *   - Redis Streams progress/result 스트림을 소비하여 잡 상태를 업데이트하고 WebSocket으로 중계한다.
 *   - 최소 재시도/데드레터 정책을 명시하기 위해 오류 시 별도 스트림으로 이동 후 ACK 처리한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/infra/v0.12.0-worker-and-queue-topology.md
 *   - design/realtime/v0.12.0-job-progress-events.md
 */
@Component
@ConditionalOnProperty(name = "jobs.queue.enabled", havingValue = "true", matchIfMissing = true)
public class JobQueueListener {

    private static final String DEAD_LETTER_STREAM = "job.deadletter";

    private final RedisConnectionFactory connectionFactory;
    private final StringRedisTemplate redisTemplate;
    private final JobQueueProperties properties;
    private final JobService jobService;
    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;

    public JobQueueListener(RedisConnectionFactory connectionFactory, StringRedisTemplate redisTemplate,
            JobQueueProperties properties, JobService jobService) {
        this.connectionFactory = connectionFactory;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.jobService = jobService;
    }

    @PostConstruct
    public void start() {
        createGroupIfMissing(properties.getProgressStream());
        createGroupIfMissing(properties.getResultStream());

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .build();
        container = StreamMessageListenerContainer.create(connectionFactory, options);

        container.receive(Consumer.from(properties.getConsumerGroup(), "progress-consumer"),
                StreamOffset.create(properties.getProgressStream(), ReadOffset.lastConsumed()),
                this::handleProgressRecord);
        container.receive(Consumer.from(properties.getConsumerGroup(), "result-consumer"),
                StreamOffset.create(properties.getResultStream(), ReadOffset.lastConsumed()),
                this::handleResultRecord);

        container.start();
    }

    @PreDestroy
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }

    private void handleProgressRecord(MapRecord<String, String, String> record) {
        try {
            Map<String, String> body = record.getValue().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            jobService.handleProgress(JobProgressMessage.fromRaw(body));
            redisTemplate.opsForStream().acknowledge(properties.getProgressStream(), properties.getConsumerGroup(), record.getId());
        } catch (Exception ex) {
            moveToDeadLetter(record, ex.getMessage());
        }
    }

    private void handleResultRecord(MapRecord<String, String, String> record) {
        try {
            Map<String, String> body = record.getValue().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            jobService.handleResult(JobResultMessage.fromRaw(body));
            redisTemplate.opsForStream().acknowledge(properties.getResultStream(), properties.getConsumerGroup(), record.getId());
        } catch (Exception ex) {
            moveToDeadLetter(record, ex.getMessage());
        }
    }

    private void createGroupIfMissing(String stream) {
        try {
            redisTemplate.opsForStream().createGroup(stream, properties.getConsumerGroup());
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("BUSYGROUP")) {
                return;
            }
            redisTemplate.opsForStream().add(stream, Map.of("bootstrap", "1"));
            redisTemplate.opsForStream().createGroup(stream, properties.getConsumerGroup());
        }
    }

    private void moveToDeadLetter(MapRecord<String, String, String> record, String reason) {
        Map<String, String> payload = record.getValue().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        payload.put("sourceStream", record.getStream());
        payload.put("recordId", record.getId().getValue());
        payload.put("reason", reason == null ? "" : reason);
        redisTemplate.opsForStream().add(DEAD_LETTER_STREAM, payload);
        redisTemplate.opsForStream().acknowledge(record.getStream(), properties.getConsumerGroup(), record.getId());
    }
}
