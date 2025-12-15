/**
 * [타입] frontend/src/shared/types/chat.ts
 * 설명:
 *   - 채팅 메시지와 히스토리 응답에 사용되는 공용 타입을 정의한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/frontend/v0.6.0-chat-ui.md
 */
export interface ChatMessage {
  id: number
  channelType: 'DM' | 'LOBBY' | 'MATCH'
  channelKey: string
  senderId: number
  senderNickname: string
  recipientId?: number | null
  content: string
  createdAt: string
}

export interface ChatHistoryResponse {
  messages: ChatMessage[]
}
