import { useCallback, useEffect, useMemo, useState } from 'react'
import { useAuth } from '../features/auth/AuthProvider'
import { useTournamentSocket } from '../hooks/useTournamentSocket'
import {
  createTournament,
  fetchTournamentDetail,
  fetchTournaments,
  joinTournament,
  startTournament,
} from '../shared/api/tournament'
import {
  TournamentDetail,
  TournamentEventMessage,
  TournamentMatch,
  TournamentSummary,
} from '../shared/types/tournament'

/**
 * [페이지] frontend/src/pages/TournamentPage.tsx
 * 설명:
 *   - 토너먼트 생성/참여/시작 기능과 단순 브래킷 뷰를 제공한다.
 *   - WebSocket 알림을 통해 매치 준비/업데이트를 실시간으로 반영한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/frontend/v0.7.0-tournament-ui.md
 *   - design/realtime/v0.7.0-tournament-events.md
 */
export function TournamentPage() {
  const { token, user } = useAuth()
  const [tournaments, setTournaments] = useState<TournamentSummary[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [detail, setDetail] = useState<TournamentDetail | null>(null)
  const [name, setName] = useState('')
  const [size, setSize] = useState(8)
  const [message, setMessage] = useState('')

  const loadList = useCallback(() => {
    fetchTournaments(token)
      .then(setTournaments)
      .catch(() => setMessage('토너먼트 목록을 불러오지 못했습니다.'))
  }, [token])

  useEffect(() => {
    loadList()
  }, [loadList])

  const loadDetail = useCallback(
    (id: number) => {
      fetchTournamentDetail(id, token)
        .then((data) => {
          setDetail(data)
          setSelectedId(id)
        })
        .catch(() => setMessage('토너먼트 세부 정보를 불러오지 못했습니다.'))
    },
    [token],
  )

  const handleSocket = useCallback(
    (msg: TournamentEventMessage) => {
      if (msg.type === 'TOURNAMENT_MATCH_READY') {
        const payload = msg.payload as { tournamentId?: number }
        if (payload.tournamentId && payload.tournamentId === selectedId) {
          loadDetail(payload.tournamentId)
        }
        loadList()
      }
      if (
        msg.type === 'TOURNAMENT_UPDATED' ||
        msg.type === 'TOURNAMENT_STARTED' ||
        msg.type === 'TOURNAMENT_COMPLETED'
      ) {
        const payload = msg.payload as TournamentDetail
        setDetail(payload)
        setSelectedId(payload.id)
        loadList()
      }
    },
    [loadDetail, loadList, selectedId],
  )

  const socketState = useTournamentSocket(token, handleSocket)

  const groupedMatches = useMemo(() => {
    if (!detail) return []
    const rounds: Record<number, TournamentMatch[]> = {}
    detail.matches.forEach((match) => {
      if (!rounds[match.round]) rounds[match.round] = []
      rounds[match.round].push(match)
    })
    return Object.entries(rounds)
      .sort((a, b) => Number(a[0]) - Number(b[0]))
      .map(([round, matches]) => ({ round: Number(round), matches: matches.sort((a, b) => a.position - b.position) }))
  }, [detail])

  const resetForm = () => {
    setName('')
    setSize(8)
  }

  const handleCreate = async () => {
    if (!name.trim()) {
      setMessage('토너먼트 이름을 입력하세요.')
      return
    }
    try {
      const created = await createTournament(name.trim(), size, token)
      setDetail(created)
      setSelectedId(created.id)
      loadList()
      resetForm()
      setMessage('토너먼트를 생성했습니다.')
    } catch (error) {
      setMessage('토너먼트를 생성하지 못했습니다.')
    }
  }

  const handleJoin = async (id: number) => {
    try {
      const joined = await joinTournament(id, token)
      setDetail(joined)
      setSelectedId(id)
      loadList()
    } catch (error) {
      setMessage('참여 요청에 실패했습니다.')
    }
  }

  const handleStart = async (id: number) => {
    try {
      const started = await startTournament(id, token)
      setDetail(started)
      setSelectedId(id)
      loadList()
    } catch (error) {
      setMessage('토너먼트를 시작하지 못했습니다.')
    }
  }

  return (
    <main className="page">
      <section className="panel">
        <h2>토너먼트 생성</h2>
        <p className="hint">4~16명, 2의 거듭제곱 인원만 지원합니다.</p>
        <div className="row">
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="토너먼트 이름"
          />
          <input
            type="number"
            min={4}
            max={16}
            value={size}
            onChange={(e) => setSize(Number(e.target.value))}
          />
          <button className="button" type="button" onClick={handleCreate}>
            생성
          </button>
        </div>
        <p className="hint">실시간 연결: {socketState.connected ? '연결됨' : '대기 중'}</p>
      </section>

      <section className="panel">
        <h2>토너먼트 목록</h2>
        <ul className="list">
          {tournaments.map((item) => (
            <li key={item.id} className="list-item">
              <div className="row">
                <strong>{item.name}</strong>
                <span>
                  {item.currentParticipants}/{item.maxParticipants}
                </span>
              </div>
              <div className="row">
                <span className="badge">{item.status}</span>
                <div className="actions">
                  <button className="button" type="button" onClick={() => loadDetail(item.id)}>
                    보기
                  </button>
                  <button className="button" type="button" onClick={() => handleJoin(item.id)}>
                    참여
                  </button>
                  {user && detail && detail.creatorId === user.id && item.status === 'REGISTRATION' && (
                    <button className="button" type="button" onClick={() => handleStart(item.id)}>
                      시작
                    </button>
                  )}
                </div>
              </div>
            </li>
          ))}
          {tournaments.length === 0 && <p>등록된 토너먼트가 없습니다.</p>}
        </ul>
      </section>

      {detail && (
        <section className="panel">
          <h2>{detail.name} 브래킷</h2>
          <p className="hint">상태: {detail.status}</p>
          <div className="row">
            <strong>참가자</strong>
            <span>
              {detail.participants.length}/{detail.maxParticipants}
            </span>
          </div>
          <ul className="list">
            {detail.participants.map((p) => (
              <li key={p.id} className="list-item">
                #{p.seed} {p.nickname}
              </li>
            ))}
          </ul>
          <div className="bracket">
            {groupedMatches.map((round) => (
              <div key={round.round} className="bracket-round">
                <h3>{round.round} 라운드</h3>
                {round.matches.map((match) => (
                  <div key={match.id} className="bracket-match">
                    <div className="row">
                      <span>{match.participantA?.nickname ?? '미정'}</span>
                      <strong>{match.scoreA ?? '-'}</strong>
                    </div>
                    <div className="row">
                      <span>{match.participantB?.nickname ?? '미정'}</span>
                      <strong>{match.scoreB ?? '-'}</strong>
                    </div>
                    <p className="hint">상태: {match.status}</p>
                    {match.roomId && <p className="hint">roomId: {match.roomId}</p>}
                  </div>
                ))}
              </div>
            ))}
            {groupedMatches.length === 0 && <p className="hint">매치가 아직 생성되지 않았습니다.</p>}
          </div>
        </section>
      )}

      {message && <p className="error">{message}</p>}
    </main>
  )
}
