package com.codexpong.backend.realtime.scaleout;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/InMemoryBackplane.java
 * 설명:
 *   - Redis 없이도 단위 테스트에서 결정적 동작을 검증하기 위한 인메모리 백플레인 구현체이다.
 *   - 실제 운영 시 Redis Streams/키 구조를 대체하지만 동일한 인터페이스 제약을 따르도록 구성했다.
 * 버전: v1.1.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-architecture.md
 *   - design/realtime/v1.1.0-gateway-shard-protocol.md
 * 변경 이력:
 *   - v1.1.0: 인메모리 구현 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public class InMemoryBackplane implements GatewayShardBackplane {

    private final Map<String, Instant> shardHeartbeats = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToShard = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<MessageEnvelope>> requestQueues = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<MessageEnvelope>> responseQueues = new ConcurrentHashMap<>();
    private final Map<String, TerminationContext> terminationContexts = new ConcurrentHashMap<>();

    @Override
    public void registerShard(String shardId, Instant now) {
        shardHeartbeats.put(shardId, now);
    }

    @Override
    public void heartbeat(String shardId, Instant now) {
        shardHeartbeats.put(shardId, now);
    }

    @Override
    public Set<String> activeShardIds(Instant now, Duration expiry) {
        Instant threshold = now.minus(expiry);
        return shardHeartbeats.entrySet().stream()
                .filter(entry -> entry.getValue().isAfter(threshold))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public void mapSessionToShard(String sessionId, String shardId, Instant now) {
        sessionToShard.put(sessionId, shardId);
        heartbeat(shardId, now);
    }

    @Override
    public Optional<String> sessionShard(String sessionId) {
        return Optional.ofNullable(sessionToShard.get(sessionId));
    }

    @Override
    public void appendRequest(String shardId, MessageEnvelope envelope) {
        requestQueues.computeIfAbsent(shardId, key -> new ConcurrentLinkedQueue<>()).add(envelope);
    }

    @Override
    public List<MessageEnvelope> drainRequests(String shardId) {
        ConcurrentLinkedQueue<MessageEnvelope> queue = requestQueues.getOrDefault(shardId, new ConcurrentLinkedQueue<>());
        List<MessageEnvelope> drained = new ArrayList<>();
        MessageEnvelope envelope;
        while ((envelope = queue.poll()) != null) {
            drained.add(envelope);
        }
        return drained;
    }

    @Override
    public void appendResponse(String sessionId, MessageEnvelope envelope) {
        responseQueues.computeIfAbsent(sessionId, key -> new ConcurrentLinkedQueue<>()).add(envelope);
    }

    @Override
    public List<MessageEnvelope> drainResponses(String sessionId) {
        ConcurrentLinkedQueue<MessageEnvelope> queue = responseQueues.getOrDefault(sessionId, new ConcurrentLinkedQueue<>());
        List<MessageEnvelope> drained = new ArrayList<>();
        MessageEnvelope envelope;
        while ((envelope = queue.poll()) != null) {
            drained.add(envelope);
        }
        return drained;
    }

    @Override
    public void recordTermination(String sessionId, TerminationContext context) {
        terminationContexts.put(sessionId, context);
    }

    @Override
    public Optional<TerminationContext> findTermination(String sessionId) {
        return Optional.ofNullable(terminationContexts.get(sessionId));
    }

    /**
     * 테스트에서 큐 상태를 살펴보기 위한 읽기 전용 스냅샷을 제공한다.
     */
    public Map<String, List<MessageEnvelope>> snapshotRequests() {
        return Collections.unmodifiableMap(requestQueues.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>(entry.getValue()))));
    }
}
