import { useEffect, useState } from 'react'
import { apiFetch } from '../shared/api/client'

interface MatchmakingResponse {
  ticketId: string
  status: 'WAITING' | 'MATCHED' | 'CANCELLED'
  roomId?: string | null
  matchType: 'NORMAL' | 'RANKED'
}

/**
 * [훅] frontend/src/hooks/useQuickMatch.ts
 * 설명:
 *   - 빠른 대전 큐 등록과 폴링 기반 상태 조회를 처리한다.
 *   - roomId가 할당되면 게임 화면으로 이동할 수 있다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/frontend/v0.4.0-ranking-and-leaderboard-ui.md
 */
export function useQuickMatch(queueType: 'normal' | 'ranked', token?: string | null) {
  const [ticketId, setTicketId] = useState<string | null>(null)
  const [roomId, setRoomId] = useState<string | null>(null)
  const [status, setStatus] = useState<'idle' | 'waiting' | 'matched' | 'error'>('idle')
  const [message, setMessage] = useState('')
  const [matchType, setMatchType] = useState<'NORMAL' | 'RANKED' | null>(queueType === 'ranked' ? 'RANKED' : 'NORMAL')

  const basePath = queueType === 'ranked' ? '/api/match/ranked' : '/api/match/quick'

  const start = async () => {
    if (!token) {
      setMessage('로그인이 필요합니다.')
      return
    }
    try {
      setMessage('매칭 대기열에 참가했습니다...')
      const response = await apiFetch<MatchmakingResponse>(basePath, { method: 'POST' }, token)
      setTicketId(response.ticketId)
      setStatus(response.status === 'MATCHED' ? 'matched' : 'waiting')
      setRoomId(response.roomId ?? null)
      setMatchType(response.matchType)
    } catch (error) {
      setMessage('매칭 요청에 실패했습니다.')
      setStatus('error')
    }
  }

  useEffect(() => {
    if (!ticketId || !token || status !== 'waiting') return
    const timer = setInterval(async () => {
      try {
        const result = await apiFetch<MatchmakingResponse>(`${basePath}/${ticketId}`, { method: 'GET' }, token)
        if (result.status === 'MATCHED') {
          setRoomId(result.roomId ?? null)
          setStatus('matched')
          setMessage('상대가 입장했습니다. 게임을 시작하세요!')
          setMatchType(result.matchType)
          clearInterval(timer)
        }
      } catch (error) {
        setMessage('매칭 상태를 확인할 수 없습니다.')
        setStatus('error')
        clearInterval(timer)
      }
    }, 1200)

    return () => clearInterval(timer)
  }, [ticketId, token, status, basePath])

  const reset = () => {
    setTicketId(null)
    setRoomId(null)
    setStatus('idle')
    setMessage('')
    setMatchType(queueType === 'ranked' ? 'RANKED' : 'NORMAL')
  }

  return {
    ticketId,
    roomId,
    status,
    message,
    matchType,
    start,
    reset,
  }
}
