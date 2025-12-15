package com.codexpong.backend.async;

import com.codexpong.backend.async.consumer.DomainEventConsumer;
import com.codexpong.backend.async.event.DomainEvent;
import com.codexpong.backend.async.event.MatchResultEventPayload;
import com.codexpong.backend.async.event.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * [테스트 설정] backend/src/test/java/com/codexpong/backend/async/FailingTestConsumerConfig.java
 * 설명:
 *   - DLQ 경로를 검증하기 위해 특정 roomId에 대해 실패를 유도하는 소비자를 주입한다.
 */
@TestConfiguration
@Profile("test")
public class FailingTestConsumerConfig {

    @Bean
    public DomainEventConsumer failingDomainEventConsumer(ObjectMapper objectMapper) {
        return new DomainEventConsumer() {
            @Override
            public String name() {
                return "test-failing";
            }

            @Override
            public boolean supports(String eventType) {
                return OutboxEventType.MATCH_RESULT_RECORDED.equals(eventType);
            }

            @Override
            public void consume(DomainEvent event) {
                try {
                    MatchResultEventPayload payload = objectMapper.treeToValue(event.payload(), MatchResultEventPayload.class);
                    if (payload.roomId().contains("dlq")) {
                        throw new IllegalStateException("테스트용 강제 실패");
                    }
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("페이로드 파싱 실패", e);
                }
            }
        };
    }
}
