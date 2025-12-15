package com.codexpong.backend.chat.domain;

/**
 * [열거형] backend/src/main/java/com/codexpong/backend/chat/domain/ChatChannelType.java
 * 설명:
 *   - 채팅 메시지가 속하는 채널 유형을 정의한다.
 *   - v0.6.0에서는 DM, 로비, 매치 방 채팅을 구분해 저장/브로드캐스트한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/backend/v0.6.0-chat-and-channels.md
 */
public enum ChatChannelType {
    DM,
    LOBBY,
    MATCH
}
