package com.codexpong.backend.async.outbox;

import com.codexpong.backend.async.consumer.DomainEventConsumer;
import com.codexpong.backend.async.event.DomainEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/async/outbox/OutboxRelayService.java
 * 설명:
 *   - 아웃박스 테이블을 스캔하여 소비자에게 전달하고 재시도/DLQ를 관리한다.
 */
@Service
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final DeadLetterEventRepository deadLetterEventRepository;
    private final List<DomainEventConsumer> consumers;
    private final ObjectMapper objectMapper;
    private final OutboxRelayProperties properties;
    private final TransactionTemplate transactionTemplate;

    public OutboxRelayService(OutboxEventRepository outboxEventRepository,
            DeadLetterEventRepository deadLetterEventRepository, List<DomainEventConsumer> consumers,
            ObjectMapper objectMapper, OutboxRelayProperties properties, TransactionTemplate transactionTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.deadLetterEventRepository = deadLetterEventRepository;
        this.consumers = consumers;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.interval-millis:1000}")
    public void scheduledPublish() {
        publishPending();
    }

    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING,
                        PageRequest.of(0, properties.getBatchSize()));
        for (OutboxEvent event : pending) {
            relay(event.getId());
        }
    }

    public void relay(Long eventId) {
        try {
            transactionTemplate.executeWithoutResult(status -> processEvent(eventId));
        } catch (Exception e) {
            registerFailure(eventId, e);
        }
    }

    private DomainEvent deserialize(OutboxEvent event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            return new DomainEvent(event.getEventId(), event.getType(), payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("아웃박스 페이로드 역직렬화에 실패했습니다.", e);
        }
    }

    private void processEvent(Long eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
        if (event == null || event.getStatus() == OutboxStatus.PUBLISHED) {
            return;
        }
        DomainEvent domainEvent = deserialize(event);
        for (DomainEventConsumer consumer : consumers) {
            if (consumer.supports(domainEvent.type())) {
                consumer.consume(domainEvent);
            }
        }
        event.markPublished();
        outboxEventRepository.save(event);
    }

    private void registerFailure(Long eventId, Exception error) {
        transactionTemplate.executeWithoutResult(status -> {
            OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() == OutboxStatus.FAILED) {
                return;
            }
            event.markAttempt(error.getMessage());
            if (event.getAttempts() >= properties.getMaxAttempts()) {
                event.markFailed(error.getMessage());
                deadLetterEventRepository.save(
                        new DeadLetterEvent(event.getEventId(), event.getType(), event.getPayload(), event.getAttempts(),
                                error.getMessage()));
            }
            outboxEventRepository.save(event);
        });
    }
}
