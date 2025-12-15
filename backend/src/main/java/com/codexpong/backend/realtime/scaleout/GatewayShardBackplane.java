package com.codexpong.backend.realtime.scaleout;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/GatewayShardBackplane.java
 * 설명:
 *   - Redis 기반 게이트웨이-샤드 백플레인을 추상화하여 테스트 친화적인 인터페이스로 제공한다.
 *   - v1.1.0에서 Redis Streams/키를 통해 필요한 상태를 저장/조회한다는 제약을 반영한다.
 * 버전: v1.1.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-architecture.md
 *   - design/realtime/v1.1.0-gateway-shard-protocol.md
 * 변경 이력:
 *   - v1.1.0: 초기 추상화 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public interface GatewayShardBackplane {

    /**
     * 샤드 등록 및 초기 하트비트를 기록한다.
     */
    void registerShard(String shardId, Instant now);

    /**
     * 샤드 하트비트를 갱신한다.
     */
    void heartbeat(String shardId, Instant now);

    /**
     * 활성 샤드 목록을 조회한다.
     */
    Set<String> activeShardIds(Instant now, Duration expiry);

    /**
     * 세션 소유권을 특정 샤드에 매핑한다.
     */
    void mapSessionToShard(String sessionId, String shardId, Instant now);

    /**
     * 세션이 속한 샤드를 반환한다.
     */
    Optional<String> sessionShard(String sessionId);

    /**
     * 게이트웨이 -> 샤드 요청 스트림에 메시지를 추가한다.
     */
    void appendRequest(String shardId, MessageEnvelope envelope);

    /**
     * 샤드별 요청을 모두 가져온다.
     */
    List<MessageEnvelope> drainRequests(String shardId);

    /**
     * 샤드 -> 게이트웨이 응답 스트림에 메시지를 추가한다.
     */
    void appendResponse(String sessionId, MessageEnvelope envelope);

    /**
     * 세션별 응답을 모두 가져온다.
     */
    List<MessageEnvelope> drainResponses(String sessionId);

    /**
     * 결정적 종료 컨텍스트를 기록한다.
     */
    void recordTermination(String sessionId, TerminationContext context);

    /**
     * 세션의 종료 컨텍스트를 조회한다.
     */
    Optional<TerminationContext> findTermination(String sessionId);
}
