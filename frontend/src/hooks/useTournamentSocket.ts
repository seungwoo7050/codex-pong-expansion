import { useEffect, useState } from 'react'
import { WS_BASE_URL } from '../constants'
import { TournamentEventMessage } from '../shared/types/tournament'

/**
 * [훅] frontend/src/hooks/useTournamentSocket.ts
 * 설명:
 *   - 토너먼트 이벤트 WebSocket을 열어 알림을 수신한다.
 *   - 서버가 전송하는 type/payload 메시지를 그대로 콜백에 전달한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/realtime/v0.7.0-tournament-events.md
 */
export function useTournamentSocket(token?: string | null, onMessage?: (message: TournamentEventMessage) => void) {
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    if (!token) return
    const socket = new WebSocket(`${WS_BASE_URL}/ws/tournament?token=${encodeURIComponent(token)}`)

    socket.onopen = () => setConnected(true)
    socket.onclose = () => setConnected(false)
    socket.onerror = () => setConnected(false)
    socket.onmessage = (event) => {
      const payload: TournamentEventMessage = JSON.parse(event.data)
      if (onMessage) onMessage(payload)
    }

    return () => socket.close()
  }, [onMessage, token])

  return { connected }
}
