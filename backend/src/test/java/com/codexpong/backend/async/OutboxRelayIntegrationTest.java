package com.codexpong.backend.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.codexpong.backend.async.consumer.AbuseSignalRepository;
import com.codexpong.backend.async.consumer.NotificationLogRepository;
import com.codexpong.backend.async.consumer.PlayerMatchStats;
import com.codexpong.backend.async.consumer.PlayerMatchStatsRepository;
import com.codexpong.backend.async.consumer.RankingProjectionRepository;
import com.codexpong.backend.async.consumer.EventConsumptionRepository;
import com.codexpong.backend.async.outbox.DeadLetterEventRepository;
import com.codexpong.backend.async.outbox.OutboxEvent;
import com.codexpong.backend.async.outbox.OutboxEventRepository;
import com.codexpong.backend.async.outbox.OutboxRelayService;
import com.codexpong.backend.async.outbox.OutboxRelayProperties;
import com.codexpong.backend.async.outbox.OutboxStatus;
import com.codexpong.backend.game.GameResultRepository;
import com.codexpong.backend.game.GameResultService;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/async/OutboxRelayIntegrationTest.java
 * 설명:
 *   - 아웃박스와 소비자/릴레이의 트랜잭션 결합, 멱등성, DLQ 경로를 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(FailingTestConsumerConfig.class)
class OutboxRelayIntegrationTest {

    @Autowired
    private GameResultService gameResultService;

    @Autowired
    private GameResultRepository gameResultRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private DeadLetterEventRepository deadLetterEventRepository;

    @Autowired
    private OutboxRelayService outboxRelayService;

    @Autowired
    private OutboxRelayProperties outboxRelayProperties;

    @Autowired
    private PlayerMatchStatsRepository statsRepository;

    @Autowired
    private RankingProjectionRepository rankingProjectionRepository;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private AbuseSignalRepository abuseSignalRepository;

    @Autowired
    private EventConsumptionRepository consumptionRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @DynamicPropertySource
    static void overrideRelayAttempts(DynamicPropertyRegistry registry) {
        registry.add("outbox.relay.max-attempts", () -> 2);
    }

    @BeforeEach
    void setUp() {
        notificationLogRepository.deleteAll();
        rankingProjectionRepository.deleteAll();
        statsRepository.deleteAll();
        abuseSignalRepository.deleteAll();
        consumptionRepository.deleteAll();
        deadLetterEventRepository.deleteAll();
        outboxEventRepository.deleteAll();
        gameResultRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void outboxAndDomainWriteRollbackTogether() {
        User playerA = userRepository.save(new User("alice", "pw", "Alice", ""));
        User playerB = userRepository.save(new User("bob", "pw", "Bob", ""));

        assertThrows(RuntimeException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            gameResultService.recordResult("room-tx", playerA, playerB, 5, 3, MatchType.RANKED,
                    LocalDateTime.now().minusMinutes(5), LocalDateTime.now());
            throw new RuntimeException("강제 롤백");
        }));

        assertEquals(0, gameResultRepository.count());
        assertEquals(0, outboxEventRepository.count());
    }

    @Test
    void consumersRemainIdempotentWhenEventIsRedelivered() {
        User playerA = userRepository.save(new User("alice", "pw", "Alice", ""));
        User playerB = userRepository.save(new User("bob", "pw", "Bob", ""));

        gameResultService.recordResult("room-dup", playerA, playerB, 7, 4, MatchType.RANKED,
                LocalDateTime.now().minusMinutes(5), LocalDateTime.now());

        outboxRelayService.publishPending();

        OutboxEvent event = outboxEventRepository.findAll().getFirst();
        PlayerMatchStats statsA = statsRepository.findByUserId(playerA.getId()).orElseThrow();
        int winsBefore = statsA.getWins();
        int notificationsBefore = notificationLogRepository.findAll().size();

        event.resetForRetry();
        outboxEventRepository.save(event);
        outboxRelayService.publishPending();

        PlayerMatchStats statsAAfter = statsRepository.findByUserId(playerA.getId()).orElseThrow();
        assertThat(statsAAfter.getWins()).isEqualTo(winsBefore);
        assertThat(notificationLogRepository.findAll()).hasSize(notificationsBefore);
    }

    @Test
    void failingConsumerRoutesEventToDlqAfterRetries() {
        User playerA = userRepository.save(new User("alice", "pw", "Alice", ""));
        User playerB = userRepository.save(new User("bob", "pw", "Bob", ""));

        gameResultService.recordResult("room-dlq", playerA, playerB, 15, 0, MatchType.NORMAL,
                LocalDateTime.now().minusMinutes(2), LocalDateTime.now());

        outboxRelayService.publishPending();
        OutboxEvent firstAttempt = outboxEventRepository.findAll().getFirst();
        assertThat(firstAttempt.getAttempts()).isEqualTo(1);
        assertThat(firstAttempt.getStatus()).isEqualTo(OutboxStatus.PENDING);

        int maxAttempts = outboxRelayProperties.getMaxAttempts();
        for (int i = 1; i < maxAttempts; i++) {
            outboxRelayService.publishPending();
        }

        OutboxEvent failed = outboxEventRepository.findById(firstAttempt.getId()).orElseThrow();
        assertThat(failed.getAttempts()).isGreaterThanOrEqualTo(maxAttempts);
        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(deadLetterEventRepository.findAll()).hasSize(1);
    }
}
