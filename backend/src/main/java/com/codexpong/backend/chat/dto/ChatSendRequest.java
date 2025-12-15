package com.codexpong.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [요청 DTO] backend/src/main/java/com/codexpong/backend/chat/dto/ChatSendRequest.java
 * 설명:
 *   - REST API로 채팅 메시지를 보낼 때 사용되는 요청 본문을 표현한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/backend/v0.6.0-chat-and-channels.md
 */
public class ChatSendRequest {

    @NotBlank
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
