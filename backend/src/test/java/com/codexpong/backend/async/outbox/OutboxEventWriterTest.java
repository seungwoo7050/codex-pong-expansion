package com.codexpong.backend.async.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/async/outbox/OutboxEventWriterTest.java
 * 설명:
 *   - 도메인 트랜잭션이 없는 경우 아웃박스 쓰기가 거부되는지 검증하고,
 *     트랜잭션 내부에서만 저장이 허용됨을 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class OutboxEventWriterTest {

    @Autowired
    private OutboxEventWriter outboxEventWriter;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clean() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void appendFailsWithoutTransaction() {
        assertThrows(IllegalTransactionStateException.class,
                () -> outboxEventWriter.append("TEST_EVENT", eventId -> new DummyPayload(eventId)));
        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void appendSucceedsWithinTransaction() {
        transactionTemplate.executeWithoutResult(status ->
                outboxEventWriter.append("TEST_EVENT", eventId -> new DummyPayload(eventId)));

        assertThat(outboxEventRepository.count()).isEqualTo(1);
    }

    private record DummyPayload(String eventId) {
    }
}
