import { apiFetch } from '../../shared/api/client'
import { ChatHistoryResponse, ChatMessage } from '../../shared/types/chat'

/**
 * [API 모듈] frontend/src/features/chat/api.ts
 * 설명:
 *   - DM, 로비, 매치 채팅 히스토리 조회와 HTTP 전송을 담당한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/frontend/v0.6.0-chat-ui.md
 */
export function fetchDmHistory(token: string, targetUserId: number) {
  return apiFetch<ChatHistoryResponse>(`/api/chat/dm/${targetUserId}`, { method: 'GET' }, token)
}

export function sendDmMessage(token: string, targetUserId: number, content: string) {
  return apiFetch<ChatMessage>(`/api/chat/dm/${targetUserId}`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  }, token)
}

export function fetchLobbyMessages(token?: string | null) {
  return apiFetch<ChatHistoryResponse>('/api/chat/lobby', { method: 'GET' }, token ?? undefined)
}

export function sendLobbyMessage(token: string, content: string) {
  return apiFetch<ChatMessage>('/api/chat/lobby', {
    method: 'POST',
    body: JSON.stringify({ content }),
  }, token)
}

export function fetchMatchMessages(roomId: string, token?: string | null) {
  return apiFetch<ChatHistoryResponse>(`/api/chat/match/${roomId}`, { method: 'GET' }, token ?? undefined)
}

export function sendMatchMessage(token: string, roomId: string, content: string) {
  return apiFetch<ChatMessage>(`/api/chat/match/${roomId}`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  }, token)
}
