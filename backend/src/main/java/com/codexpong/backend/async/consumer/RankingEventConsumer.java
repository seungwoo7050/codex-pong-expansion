package com.codexpong.backend.async.consumer;

import com.codexpong.backend.async.event.DomainEvent;
import com.codexpong.backend.async.event.MatchResultEventPayload;
import com.codexpong.backend.async.event.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * [컴포넌트] backend/src/main/java/com/codexpong/backend/async/consumer/RankingEventConsumer.java
 * 설명:
 *   - 경기 결과 이벤트를 받아 최신 레이팅 프로젝션을 갱신한다.
 */
@Component
public class RankingEventConsumer implements DomainEventConsumer {

    private final RankingProjectionRepository projectionRepository;
    private final EventConsumptionRepository consumptionRepository;
    private final ObjectMapper objectMapper;

    public RankingEventConsumer(RankingProjectionRepository projectionRepository,
            EventConsumptionRepository consumptionRepository, ObjectMapper objectMapper) {
        this.projectionRepository = projectionRepository;
        this.consumptionRepository = consumptionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "ranking";
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
        upsert(payload.playerAId(), payload.ratingAfterA(), payload.eventId());
        upsert(payload.playerBId(), payload.ratingAfterB(), payload.eventId());
        consumptionRepository.save(new EventConsumption(event.eventId(), name()));
    }

    private MatchResultEventPayload deserialize(DomainEvent event) {
        try {
            return objectMapper.treeToValue(event.payload(), MatchResultEventPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("매치 결과 페이로드 역직렬화에 실패했습니다.", e);
        }
    }

    private void upsert(Long userId, int rating, String eventId) {
        projectionRepository.findByUserId(userId)
                .ifPresentOrElse(existing -> existing.refresh(rating, eventId),
                        () -> projectionRepository.save(new RankingProjection(userId, rating, eventId)));
    }
}
