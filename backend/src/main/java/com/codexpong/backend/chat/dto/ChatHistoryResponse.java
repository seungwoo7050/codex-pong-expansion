package com.codexpong.backend.chat.dto;

import java.util.List;

/**
 * [응답 DTO] backend/src/main/java/com/codexpong/backend/chat/dto/ChatHistoryResponse.java
 * 설명:
 *   - 특정 채널의 메시지 목록을 전달한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/backend/v0.6.0-chat-and-channels.md
 */
public class ChatHistoryResponse {

    private List<ChatMessageResponse> messages;

    public ChatHistoryResponse(List<ChatMessageResponse> messages) {
        this.messages = messages;
    }

    public List<ChatMessageResponse> getMessages() {
        return messages;
    }
}
