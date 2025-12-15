import { useCallback, useEffect, useState } from 'react'
import { fetchLiveMatches } from '../features/game/api'
import { LiveMatchSummary } from '../shared/types/game'

/**
 * [훅] frontend/src/hooks/useLiveMatches.ts
 * 설명:
 *   - 백엔드의 진행 중 경기 목록을 주기적으로 조회해 관전 진입 버튼에 활용한다.
 * 버전: v0.8.0
 * 관련 설계문서:
 *   - design/frontend/v0.8.0-spectator-ui.md
 */
export function useLiveMatches(token?: string | null) {
  const [liveMatches, setLiveMatches] = useState<LiveMatchSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    if (!token) return
    setLoading(true)
    fetchLiveMatches(token)
      .then((res) => {
        setLiveMatches(res)
        setError('')
      })
      .catch(() => setError('진행 중 경기 정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [token])

  useEffect(() => {
    load()
    if (!token) return undefined
    const timer = window.setInterval(load, 5000)
    return () => window.clearInterval(timer)
  }, [token, load])

  return { liveMatches, loading, error, refresh: load }
}
