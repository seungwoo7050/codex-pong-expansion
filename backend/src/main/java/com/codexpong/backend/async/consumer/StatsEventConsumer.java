package com.codexpong.backend.async.consumer;

import com.codexpong.backend.async.event.DomainEvent;
import com.codexpong.backend.async.event.MatchResultEventPayload;
import com.codexpong.backend.async.event.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * [컴포넌트] backend/src/main/java/com/codexpong/backend/async/consumer/StatsEventConsumer.java
 * 설명:
 *   - 경기 결과를 받아 사용자별 승/패/무 집계를 업데이트한다.
 */
@Component
public class StatsEventConsumer implements DomainEventConsumer {

    private final PlayerMatchStatsRepository statsRepository;
    private final EventConsumptionRepository consumptionRepository;
    private final ObjectMapper objectMapper;

    public StatsEventConsumer(PlayerMatchStatsRepository statsRepository,
            EventConsumptionRepository consumptionRepository, ObjectMapper objectMapper) {
        this.statsRepository = statsRepository;
        this.consumptionRepository = consumptionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "stats";
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
        if (payload.scoreA() > payload.scoreB()) {
            recordWin(payload.playerAId(), payload.eventId());
            recordLoss(payload.playerBId(), payload.eventId());
        } else if (payload.scoreB() > payload.scoreA()) {
            recordWin(payload.playerBId(), payload.eventId());
            recordLoss(payload.playerAId(), payload.eventId());
        } else {
            recordDraw(payload.playerAId(), payload.eventId());
            recordDraw(payload.playerBId(), payload.eventId());
        }
        consumptionRepository.save(new EventConsumption(event.eventId(), name()));
    }

    private MatchResultEventPayload deserialize(DomainEvent event) {
        try {
            return objectMapper.treeToValue(event.payload(), MatchResultEventPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("매치 결과 페이로드 역직렬화에 실패했습니다.", e);
        }
    }

    private void recordWin(Long userId, String eventId) {
        statsRepository.findByUserId(userId)
                .ifPresentOrElse(existing -> existing.recordWin(eventId),
                        () -> {
                            PlayerMatchStats created = new PlayerMatchStats(userId, eventId);
                            created.recordWin(eventId);
                            statsRepository.save(created);
                        });
    }

    private void recordLoss(Long userId, String eventId) {
        statsRepository.findByUserId(userId)
                .ifPresentOrElse(existing -> existing.recordLoss(eventId),
                        () -> {
                            PlayerMatchStats created = new PlayerMatchStats(userId, eventId);
                            created.recordLoss(eventId);
                            statsRepository.save(created);
                        });
    }

    private void recordDraw(Long userId, String eventId) {
        statsRepository.findByUserId(userId)
                .ifPresentOrElse(existing -> existing.recordDraw(eventId),
                        () -> {
                            PlayerMatchStats created = new PlayerMatchStats(userId, eventId);
                            created.recordDraw(eventId);
                            statsRepository.save(created);
                        });
    }
}
