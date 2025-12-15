package com.codexpong.backend.realtime.scaleout;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * [모듈] backend/src/main/java/com/codexpong/backend/realtime/scaleout/MapBuilder.java
 * 설명:
 *   - 테스트와 메시지 생성 시 간결하게 불변 Map을 만들기 위한 헬퍼이다.
 * 버전: v1.1.0
 * 관련 설계문서:
 *   - design/realtime/v1.1.0-protocol.md
 * 변경 이력:
 *   - v1.1.0: 불변 Map 생성기 추가
 * 테스트:
 *   - backend/src/test/java/com/codexpong/backend/realtime/scaleout/GatewayShardScaleOutTest.java
 */
public final class MapBuilder {

    private MapBuilder() {
    }

    public static Map<String, Object> of(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, Object> empty() {
        return Collections.emptyMap();
    }
}
