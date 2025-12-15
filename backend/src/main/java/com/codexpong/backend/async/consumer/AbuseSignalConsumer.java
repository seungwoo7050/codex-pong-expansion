package com.codexpong.backend.async.consumer;

import com.codexpong.backend.async.event.DomainEvent;
import com.codexpong.backend.async.event.MatchResultEventPayload;
import com.codexpong.backend.async.event.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * [컴포넌트] backend/src/main/java/com/codexpong/backend/async/consumer/AbuseSignalConsumer.java
 * 설명:
 *   - 단순 휴리스틱으로 점수 조작/던지기 의심 사례를 기록한다.
 */
@Component
public class AbuseSignalConsumer implements DomainEventConsumer {

    private final AbuseSignalRepository abuseSignalRepository;
    private final EventConsumptionRepository consumptionRepository;
    private final ObjectMapper objectMapper;

    public AbuseSignalConsumer(AbuseSignalRepository abuseSignalRepository,
            EventConsumptionRepository consumptionRepository, ObjectMapper objectMapper) {
        this.abuseSignalRepository = abuseSignalRepository;
        this.consumptionRepository = consumptionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "abuse-signals";
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
        int scoreDiff = Math.abs(payload.scoreA() - payload.scoreB());
        if (scoreDiff >= 10) {
            Long loser = payload.scoreA() > payload.scoreB() ? payload.playerBId() : payload.playerAId();
            abuseSignalRepository.save(new AbuseSignal(payload.eventId(), payload.roomId(), loser, "대량 점수 차이"));
        } else if (Math.abs(payload.ratingChangeA()) >= 50 || Math.abs(payload.ratingChangeB()) >= 50) {
            Long target = payload.ratingChangeA() > payload.ratingChangeB() ? payload.playerAId() : payload.playerBId();
            abuseSignalRepository.save(new AbuseSignal(payload.eventId(), payload.roomId(), target, "비정상 레이팅 변동"));
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
}
