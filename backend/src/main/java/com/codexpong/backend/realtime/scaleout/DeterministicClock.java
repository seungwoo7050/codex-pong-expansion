package com.codexpong.backend.realtime.scaleout;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/DeterministicClock.java
 * 설명:
 *   - sleep 없이 게임 루프와 하트비트 시뮬레이션을 진행하기 위한 스텝 기반 Clock 구현체이다.
 * 버전: v1.1.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-architecture.md
 * 변경 이력:
 *   - v1.1.0: 결정적 Clock 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public class DeterministicClock extends Clock {

    private Instant current;
    private final ZoneId zoneId;

    public DeterministicClock(Instant start, ZoneId zoneId) {
        this.current = start;
        this.zoneId = zoneId;
    }

    /**
     * 지정한 초 단위만큼 시계를 전진시킨다.
     */
    public void advanceSeconds(long seconds) {
        current = current.plusSeconds(seconds);
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new DeterministicClock(current, zone);
    }

    @Override
    public Instant instant() {
        return current;
    }
}
