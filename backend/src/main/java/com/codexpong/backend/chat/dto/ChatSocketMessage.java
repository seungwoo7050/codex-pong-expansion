package com.codexpong.backend.chat.dto;

/**
 * [소켓 DTO] backend/src/main/java/com/codexpong/backend/chat/dto/ChatSocketMessage.java
 * 설명:
 *   - WebSocket을 통해 수신되는 채팅 명령을 표현한다.
 *   - type 필드를 기반으로 DM/로비/매치 전송 또는 채널 구독을 처리한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/realtime/v0.6.0-chat-events.md
 */
public class ChatSocketMessage {

    private String type;
    private Long targetUserId;
    private String roomId;
    private String content;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
