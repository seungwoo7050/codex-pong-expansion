import { useEffect, useRef, useState } from 'react'
import { WS_BASE_URL } from '../constants'
import { ChatMessage } from '../shared/types/chat'

/**
 * [훅] frontend/src/hooks/useChatSocket.ts
 * 설명:
 *   - 채팅 WebSocket을 열고 수신 메시지를 콜백으로 전달한다.
 *   - 로비 채널은 자동 구독하며, 매치 방은 subscribeMatch로 별도 구독한다.
 * 버전: v0.6.0
 * 관련 설계문서:
 *   - design/frontend/v0.6.0-chat-ui.md
 *   - design/realtime/v0.6.0-chat-events.md
 */
export function useChatSocket(token?: string | null, onMessage?: (msg: ChatMessage) => void) {
  const [connected, setConnected] = useState(false)
  const socketRef = useRef<WebSocket | null>(null)

  useEffect(() => {
    if (!token) return
    const socket = new WebSocket(`${WS_BASE_URL}/ws/chat?token=${encodeURIComponent(token)}`)
    socketRef.current = socket

    socket.onopen = () => setConnected(true)
    socket.onclose = () => setConnected(false)
    socket.onerror = () => setConnected(false)
    socket.onmessage = (event) => {
      const payload: ChatMessage = JSON.parse(event.data)
      if (onMessage) onMessage(payload)
    }

    return () => {
      socket.close()
    }
  }, [onMessage, token])

  const sendCommand = (command: unknown) => {
    if (!socketRef.current || socketRef.current.readyState !== WebSocket.OPEN) return
    socketRef.current.send(JSON.stringify(command))
  }

  const sendDm = (targetUserId: number, content: string) =>
    sendCommand({ type: 'DM_SEND', targetUserId, content })

  const sendLobby = (content: string) => sendCommand({ type: 'LOBBY_SEND', content })

  const sendMatch = (roomId: string, content: string) =>
    sendCommand({ type: 'MATCH_SEND', roomId, content })

  const subscribeMatch = (roomId: string) => sendCommand({ type: 'SUBSCRIBE_MATCH', roomId })

  return { connected, sendDm, sendLobby, sendMatch, subscribeMatch }
}
