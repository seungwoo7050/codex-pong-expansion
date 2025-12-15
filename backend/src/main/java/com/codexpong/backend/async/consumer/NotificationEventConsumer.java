package com.codexpong.backend.async.consumer;

import com.codexpong.backend.async.event.DomainEvent;
import com.codexpong.backend.async.event.MatchResultEventPayload;
import com.codexpong.backend.async.event.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * [컴포넌트] backend/src/main/java/com/codexpong/backend/async/consumer/NotificationEventConsumer.java
 * 설명:
 *   - 경기 결과를 간단한 알림 메시지로 적재해 사용자 피드에 활용한다.
 */
@Component
public class NotificationEventConsumer implements DomainEventConsumer {

    private final NotificationLogRepository notificationLogRepository;
    private final EventConsumptionRepository consumptionRepository;
    private final ObjectMapper objectMapper;

    public NotificationEventConsumer(NotificationLogRepository notificationLogRepository,
            EventConsumptionRepository consumptionRepository, ObjectMapper objectMapper) {
        this.notificationLogRepository = notificationLogRepository;
        this.consumptionRepository = consumptionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "notifications";
    }

    @Override
    public boolean supports(String eventType) {
        return OutboxEventType.MATCH_RESULT_RECORDED.equals(eventType);
    }

    @Override
    @Transactional
    public void consume(DomainEvent event) {
        if (consumptionRepository.existsById(new EventConsumptionId(event.eventId(), name()))) {
            return;
        }
        MatchResultEventPayload payload = deserialize(event);
        String summary = "매치 종료: " + payload.roomId() + " (" + payload.scoreA() + "-" + payload.scoreB() + ")";
        persistIfMissing(payload.eventId(), payload.playerAId(), summary);
        persistIfMissing(payload.eventId(), payload.playerBId(), summary);
        consumptionRepository.save(new EventConsumption(event.eventId(), name()));
    }

    private MatchResultEventPayload deserialize(DomainEvent event) {
        try {
            return objectMapper.treeToValue(event.payload(), MatchResultEventPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("매치 결과 페이로드 역직렬화에 실패했습니다.", e);
        }
    }

    private void persistIfMissing(String eventId, Long userId, String message) {
        if (notificationLogRepository.findByEventIdAndUserId(eventId, userId).isEmpty()) {
            notificationLogRepository.save(new NotificationLog(userId, eventId, message));
        }
    }
}
