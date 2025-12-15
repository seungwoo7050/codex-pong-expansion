import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthProvider'
import { useLiveMatches } from '../hooks/useLiveMatches'
import { LiveMatchSummary } from '../shared/types/game'
import { apiFetch } from '../shared/api/client'
import { toTierLabel } from '../shared/utils/rating'

interface LeaderboardEntry {
  rank: number
  userId: number
  nickname: string
  rating: number
  avatarUrl?: string | null
}

/**
 * [페이지] frontend/src/pages/LeaderboardPage.tsx
 * 설명:
 *   - v0.4.0 기본 리더보드 화면으로 상위 레이팅 사용자를 보여준다.
 *   - 티어 라벨과 레이팅 점수를 함께 노출해 랭크 상황을 한눈에 파악한다.
 *   - v0.8.0에서는 경기 중인 사용자를 관전할 수 있는 버튼을 함께 노출한다.
 * 버전: v0.14.0
 * 관련 설계문서:
 *   - design/frontend/v0.8.0-spectator-ui.md
 *   - design/frontend/v0.14.0-reflow-audit-and-fixes.md
 */
export function LeaderboardPage() {
  const { token } = useAuth()
  const [entries, setEntries] = useState<LeaderboardEntry[]>([])
  const [error, setError] = useState('')
  const [pageSize, setPageSize] = useState(20)
  const [pageIndex, setPageIndex] = useState(0)
  const navigate = useNavigate()
  const { liveMatches } = useLiveMatches(token)

  const liveMatchMap = useMemo(() => {
    const map = new Map<number, LiveMatchSummary>()
    liveMatches.forEach((match) => {
      map.set(match.leftPlayerId, match)
      map.set(match.rightPlayerId, match)
    })
    return map
  }, [liveMatches])

  const liveMatchFor = (userId: number) => liveMatchMap.get(userId)

  const totalPages = useMemo(() => Math.max(1, Math.ceil(entries.length / pageSize)), [entries.length, pageSize])

  const pagedEntries = useMemo(
    () => entries.slice(pageIndex * pageSize, pageIndex * pageSize + pageSize),
    [entries, pageIndex, pageSize],
  )

  useEffect(() => {
    if (!token) return
    apiFetch<LeaderboardEntry[]>('/api/rank/leaderboard', { method: 'GET' }, token)
      .then(setEntries)
      .catch(() => setError('리더보드를 불러오지 못했습니다.'))
  }, [token])

  useEffect(() => {
    setPageIndex(0)
  }, [pageSize, entries.length])

  return (
    <main className="page">
      <section className="panel">
        <h2>글로벌 리더보드</h2>
        {error && <p className="error">{error}</p>}
        <div className="inline-form">
          <label>
            페이지 크기
            <select value={pageSize} onChange={(e) => setPageSize(Number(e.target.value))}>
              {[10, 20, 50].map((size) => (
                <option key={size} value={size}>
                  {size}개씩 보기
                </option>
              ))}
            </select>
          </label>
          <div className="pager">
            <button type="button" className="secondary" disabled={pageIndex === 0} onClick={() => setPageIndex((prev) => Math.max(0, prev - 1))}>
              이전
            </button>
            <span>
              {pageIndex + 1} / {totalPages}
            </span>
            <button
              type="button"
              className="secondary"
              disabled={pageIndex >= totalPages - 1}
              onClick={() => setPageIndex((prev) => Math.min(totalPages - 1, prev + 1))}
            >
              다음
            </button>
          </div>
        </div>
        <ul className="list virtualized">
          {pagedEntries.map((entry) => (
            <li key={entry.userId} className="list-item">
              <div className="row">
                <strong>
                  #{entry.rank} {entry.nickname}
                </strong>
                <span className="score">{entry.rating}점</span>
              </div>
              <div className="row">
                <small>티어: {toTierLabel(entry.rating)}</small>
                {entry.avatarUrl && <small>아바타: {entry.avatarUrl}</small>}
              </div>
              {liveMatchFor(entry.userId) && (
                <div className="actions">
                  <button
                    className="button"
                    type="button"
                    onClick={() => navigate(`/spectate?roomId=${liveMatchFor(entry.userId)?.roomId}`)}
                  >
                    관전하기
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
        {entries.length === 0 && !error && <p>아직 리더보드 정보가 없습니다.</p>}
      </section>
    </main>
  )
}
