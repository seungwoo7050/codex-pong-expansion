package com.codexpong.backend.async.event;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * [레코드] backend/src/main/java/com/codexpong/backend/async/event/DomainEvent.java
 * 설명:
 *   - 아웃박스에서 읽어 소비자에게 전달되는 이벤트 공용 포맷이다.
 */
public record DomainEvent(String eventId, String type, JsonNode payload) {
}
