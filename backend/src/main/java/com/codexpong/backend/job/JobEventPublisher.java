package com.codexpong.backend.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * [이벤트 퍼블리셔] backend/src/main/java/com/codexpong/backend/job/JobEventPublisher.java
 * 설명:
 *   - 잡 진행률/결과를 사용자별 WebSocket 세션으로 전달한다.
 *   - 세션 연결 상태가 불안정해도 백엔드 처리 흐름을 막지 않도록 예외는 무시한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/realtime/v0.12.0-job-progress-events.md
 */
@Component
public class JobEventPublisher {

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public JobEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(session);
        if (userSessions.isEmpty()) {
            sessions.remove(userId);
        }
    }

    public void publishProgress(Long userId, JobProgressMessage message) {
        publish(userId, "job.progress", Map.of(
                "jobId", message.jobId(),
                "progress", message.progress(),
                "phase", message.phase(),
                "message", message.message()
        ));
    }

    public void publishCompleted(Long userId, JobResultMessage message) {
        publish(userId, "job.completed", Map.of(
                "jobId", message.jobId(),
                "downloadUrl", "/api/jobs/" + message.jobId() + "/result",
                "checksum", message.checksum()
        ));
    }

    public void publishFailed(Long userId, JobResultMessage message) {
        publish(userId, "job.failed", Map.of(
                "jobId", message.jobId(),
                "errorCode", message.errorCode(),
                "errorMessage", message.errorMessage()
        ));
    }

    private void publish(Long userId, String type, Object payload) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "payload", payload
            ));
            TextMessage message = new TextMessage(body);
            userSessions.forEach(session -> sendSafely(session, message));
        } catch (IOException ignored) {
        }
    }

    private void sendSafely(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException ignored) {
        }
    }
}
