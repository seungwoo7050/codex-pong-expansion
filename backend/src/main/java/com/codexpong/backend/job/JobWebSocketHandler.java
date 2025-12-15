package com.codexpong.backend.job;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.config.WebSocketAuthHandshakeInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * [웹소켓 핸들러] backend/src/main/java/com/codexpong/backend/job/JobWebSocketHandler.java
 * 설명:
 *   - 잡 진행률/결과 알림을 받을 사용자의 세션을 등록한다.
 *   - 별도 수신 명령은 없으며 연결 유지/해제 이벤트만 처리한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/realtime/v0.12.0-job-progress-events.md
 */
@Component
public class JobWebSocketHandler extends TextWebSocketHandler {

    private final JobEventPublisher jobEventPublisher;

    public JobWebSocketHandler(JobEventPublisher jobEventPublisher) {
        this.jobEventPublisher = jobEventPublisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        AuthenticatedUser user = (AuthenticatedUser) session.getAttributes()
                .get(WebSocketAuthHandshakeInterceptor.AUTH_USER_KEY);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("인증이 필요합니다."));
            return;
        }
        jobEventPublisher.register(user.id(), session);
        session.sendMessage(new TextMessage("{\"type\":\"job.connected\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        AuthenticatedUser user = (AuthenticatedUser) session.getAttributes()
                .get(WebSocketAuthHandshakeInterceptor.AUTH_USER_KEY);
        if (user != null) {
            jobEventPublisher.unregister(user.id(), session);
        }
    }
}
