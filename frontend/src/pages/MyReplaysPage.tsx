import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthProvider'
import { fetchReplays } from '../features/replay/api'
import { ReplaySummary } from '../shared/types/replay'

/**
 * [페이지] frontend/src/pages/MyReplaysPage.tsx
 * 설명:
 *   - v0.12.0에서는 내보내기 잡 페이지로 빠르게 이동할 수 있는 링크를 추가했다.
 *   - 상대 닉네임으로 필터링하고 결과/길이/생성 시간을 확인한 뒤 뷰어로 이동할 수 있다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/frontend/v0.11.0-replay-browser-and-viewer.md
 *   - design/frontend/v0.12.0-replay-export-and-jobs-ui.md
 */
export function MyReplaysPage() {
  const { token } = useAuth()
  const [replays, setReplays] = useState<ReplaySummary[]>([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) return
    setLoading(true)
    fetchReplays(token)
      .then((res) => {
        setReplays(res.items)
        setError('')
      })
      .catch(() => setError('리플레이 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [token])

  const filtered = useMemo(
    () => replays.filter((item) => item.opponentNickname.toLowerCase().includes(query.toLowerCase())),
    [query, replays],
  )

  const formatDuration = (ms: number) => {
    const totalSeconds = Math.round(ms / 1000)
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    return `${minutes}:${seconds.toString().padStart(2, '0')}`
  }

  const resultLabel = (replay: ReplaySummary) => {
    if (replay.myScore === replay.opponentScore) return '무승부'
    return replay.myScore > replay.opponentScore ? '승리' : '패배'
  }

  return (
    <main className="page">
      <section className="panel">
        <h2>내 리플레이</h2>
        <p>매치 종료 후 저장된 리플레이를 확인하고 재생할 수 있습니다.</p>
        <div className="toolbar">
          <input
            placeholder="상대 닉네임 검색"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <Link className="secondary" to="/jobs">
            내 내보내기 잡 보기
          </Link>
        </div>
        {loading && <p>불러오는 중...</p>}
        {error && <p className="error">{error}</p>}
        {!loading && filtered.length === 0 && <p className="hint">리플레이가 없습니다.</p>}
        <table className="table">
          <thead>
            <tr>
              <th>날짜</th>
              <th>상대</th>
              <th>결과</th>
              <th>길이</th>
              <th>보기</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((replay) => (
              <tr key={replay.replayId}>
                <td>{new Date(replay.createdAt).toLocaleString('ko-KR')}</td>
                <td>{replay.opponentNickname}</td>
                <td>
                  {replay.myScore} : {replay.opponentScore} ({resultLabel(replay)})
                </td>
                <td>{formatDuration(replay.durationMs)}</td>
                <td>
                  <Link className="button" to={`/replays/${replay.replayId}`}>
                    뷰어 열기
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </main>
  )
}
