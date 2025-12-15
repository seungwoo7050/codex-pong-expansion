import { useEffect, useRef, useState } from 'react'
import { WS_BASE_URL } from '../constants'

/**
 * [훅] frontend/src/hooks/useEchoSocket.ts
 * 설명:
 *   - WebSocket 연결을 맺고 에코 메시지를 주고받는 최소 로직을 제공한다.
 *   - v0.2.0에서는 JWT 토큰을 쿼리 파라미터로 전달해 인증된 연결을 시도한다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/realtime/v0.1.0-basic-websocket-wiring.md
 *   - design/backend/v0.2.0-auth-and-profile.md
 * 변경 이력:
 *   - v0.1.0: 에코 연결/전송/수신 상태 관리 추가
 *   - v0.2.0: 인증 토큰을 포함한 연결 시도
 */
export function useEchoSocket(token?: string | null) {
  const [messages, setMessages] = useState<string[]>([])
  const [connected, setConnected] = useState(false)
  const [error, setError] = useState('')
  const socketRef = useRef<WebSocket | null>(null)

  useEffect(() => {
    if (!token) return

    const socket = new WebSocket(`${WS_BASE_URL}/ws/echo?token=${encodeURIComponent(token)}`)
    socketRef.current = socket

    socket.onopen = () => {
      setError('')
      setConnected(true)
    }
    socket.onclose = () => {
      setConnected(false)
    }
    socket.onmessage = (event) => {
      setMessages((prev) => [...prev, event.data])
    }
    socket.onerror = () => setError('웹소켓 연결에 실패했습니다.')

    return () => {
      socket.close()
    }
  }, [token])

  const sendMessage = (text: string) => {
    if (socketRef.current && connected) {
      socketRef.current.send(text)
    }
  }

  return {
    connected,
    messages,
    error,
    sendMessage,
  }
}
