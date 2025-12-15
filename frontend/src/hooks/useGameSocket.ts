import { useEffect, useRef, useState } from 'react'
import { WS_BASE_URL } from '../constants'
import { GameServerMessage, GameSnapshot, RatingChange } from '../shared/types/game'

/**
 * [훅] frontend/src/hooks/useGameSocket.ts
 * 설명:
 *   - 주어진 roomId와 토큰으로 게임 WebSocket을 연결하고 상태 스냅샷을 관리한다.
 *   - 입력 방향을 서버에 전송하는 헬퍼를 제공한다.
 *   - v0.8.0에서는 관전자 역할 구분과 관전자 수 상태를 반환한다.
 * 버전: v0.8.0
 * 관련 설계문서:
 *   - design/frontend/v0.8.0-spectator-ui.md
 *   - design/realtime/v0.8.0-spectator-events.md
 */
export function useGameSocket(
  roomId?: string | null,
  token?: string | null,
  audience: 'PLAYER' | 'SPECTATOR' = 'PLAYER',
) {
  const [connected, setConnected] = useState(false)
  const [error, setError] = useState('')
  const [snapshot, setSnapshot] = useState<GameSnapshot | null>(null)
  const [matchType, setMatchType] = useState<'NORMAL' | 'RANKED' | null>(null)
  const [ratingChange, setRatingChange] = useState<RatingChange | null>(null)
  const [audienceRole, setAudienceRole] = useState<'PLAYER' | 'SPECTATOR'>(audience)
  const [spectatorCount, setSpectatorCount] = useState(0)
  const socketRef = useRef<WebSocket | null>(null)

  useEffect(() => {
    if (!roomId || !token) return

    setSnapshot(null)
    setMatchType(null)
    setRatingChange(null)
    setSpectatorCount(0)

    const socket = new WebSocket(
      `${WS_BASE_URL}/ws/game?roomId=${encodeURIComponent(roomId)}&token=${encodeURIComponent(token)}&role=${audience}`,
    )
    socketRef.current = socket

    socket.onopen = () => {
      setConnected(true)
      setError('')
    }
    socket.onclose = () => setConnected(false)
    socket.onerror = () => setError('실시간 연결에 실패했습니다.')
    socket.onmessage = (event) => {
      const data: GameServerMessage = JSON.parse(event.data)
      setSnapshot(data.snapshot)
      setMatchType(data.matchType)
      if (data.ratingChange) {
        setRatingChange(data.ratingChange)
      }
      if (data.audienceRole) {
        setAudienceRole(data.audienceRole)
      }
      if (typeof data.spectatorCount === 'number') {
        setSpectatorCount(data.spectatorCount)
      }
    }

    return () => {
      socket.close()
    }
  }, [roomId, token, audience])

  const sendInput = (direction: 'UP' | 'DOWN' | 'STAY') => {
    if (
      !socketRef.current ||
      socketRef.current.readyState !== WebSocket.OPEN ||
      !roomId ||
      audienceRole === 'SPECTATOR'
    )
      return
    const payload = {
      type: 'INPUT',
      roomId,
      direction,
    }
    socketRef.current.send(JSON.stringify(payload))
  }

  return { connected, error, snapshot, matchType, ratingChange, audienceRole, spectatorCount, sendInput }
}
