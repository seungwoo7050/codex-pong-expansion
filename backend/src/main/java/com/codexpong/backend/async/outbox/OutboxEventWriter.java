package com.codexpong.backend.async.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * [컴포넌트] backend/src/main/java/com/codexpong/backend/async/outbox/OutboxEventWriter.java
 * 설명:
 *   - 도메인 트랜잭션 안에서 아웃박스 이벤트를 직렬화해 저장한다.
 */
@Component
public class OutboxEventWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventWriter(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OutboxEvent append(String type, Function<String, Object> payloadBuilder) {
        try {
            OutboxEvent event = new OutboxEvent(type, "{}");
            Object payload = payloadBuilder.apply(event.getEventId());
            event.overridePayload(objectMapper.writeValueAsString(payload));
            return outboxEventRepository.save(event);
        } catch (Exception e) {
            throw new IllegalArgumentException("아웃박스 직렬화에 실패했습니다.", e);
        }
    }
}
