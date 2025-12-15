import { useMemo } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthProvider'
import { useGameSocket } from '../hooks/useGameSocket'
import { useLiveMatches } from '../hooks/useLiveMatches'
import { GameSnapshot } from '../shared/types/game'

/**
 * [페이지] frontend/src/pages/SpectatorPage.tsx
 * 설명:
 *   - 진행 중 경기 목록을 보여주고 roomId를 선택해 관전 모드로 입장한다.
 *   - 관전자는 패들 입력 없이 상태만 수신하며, 관전자 수/지연 정책을 UI에 안내한다.
 * 버전: v0.8.0
 * 관련 설계문서:
 *   - design/frontend/v0.8.0-spectator-ui.md
 *   - design/realtime/v0.8.0-spectator-events.md
 */
export function SpectatorPage() {
  const { token } = useAuth()
  const [params, setParams] = useSearchParams()
  const roomId = params.get('roomId')
  const navigate = useNavigate()
  const { liveMatches, loading, error, refresh } = useLiveMatches(token)
  const { connected, snapshot, matchType, spectatorCount } = useGameSocket(roomId, token, 'SPECTATOR')

  const selectedMatch = useMemo(() => liveMatches.find((m) => m.roomId === roomId), [liveMatches, roomId])

  const renderCourt = (state: GameSnapshot) => {
    const scale = 0.6
    const courtWidth = 800 * scale
    const courtHeight = 480 * scale
    const paddleHeight = 80 * scale
    const paddleWidth = 12
    const ballSize = 12

    return (
      <div className="court" style={{ width: courtWidth, height: courtHeight }}>
        <div
          className="paddle left"
          style={{ height: paddleHeight, width: paddleWidth, top: state.leftPaddleY * scale }}
        />
        <div
          className="paddle right"
          style={{ height: paddleHeight, width: paddleWidth, top: state.rightPaddleY * scale, right: 0 }}
        />
        <div className="ball" style={{ width: ballSize, height: ballSize, left: state.ballX * scale, top: state.ballY * scale }} />
      </div>
    )
  }

  return (
    <main className="page">
      <section className="panel">
        <h2>실시간 관전</h2>
        <p className="hint">목록에서 방을 선택하면 입력 없이 상태만 시청합니다. 관전자 지연 전송이 적용됩니다.</p>
        <div className="row">
          <button className="secondary" type="button" onClick={refresh} disabled={loading}>
            새로고침
          </button>
          {error && <span className="error">{error}</span>}
        </div>
        <ul className="list">
          {liveMatches.map((match) => (
            <li key={match.roomId} className="list-item">
              <div className="row">
                <strong>
                  {match.leftNickname} vs {match.rightNickname}
                </strong>
                <span className="badge">{match.matchType === 'RANKED' ? '랭크' : '일반'}</span>
              </div>
              <div className="row">
                <small>관전자 {match.spectatorCount} / {match.spectatorLimit}</small>
                <small>roomId: {match.roomId}</small>
              </div>
              <div className="actions">
                <button className="button" type="button" onClick={() => setParams({ roomId: match.roomId })}>
                  관전하기
                </button>
              </div>
            </li>
          ))}
        </ul>
        {liveMatches.length === 0 && !loading && <p>현재 진행 중인 경기가 없습니다.</p>}
      </section>

      <section className="panel">
        <h3>관전 화면</h3>
        {!roomId && <p className="hint">관전할 방을 선택하세요.</p>}
        {roomId && (
          <div>
            <p>
              방 번호: {roomId} / 연결 상태: {connected ? '연결됨' : '대기 중'} / 타입: {matchType || '알 수 없음'} / 관전자:
              {spectatorCount}
            </p>
            {!selectedMatch && <p className="hint">목록에 없는 방입니다. 로비에서 다시 선택하세요.</p>}
            {snapshot ? (
              <div className="game-area">
                {renderCourt(snapshot)}
                <div className="scoreboard">
                  <div className="score">왼쪽: {snapshot.leftScore}</div>
                  <div className="score">오른쪽: {snapshot.rightScore}</div>
                  <div className="score">목표: {snapshot.targetScore}</div>
                </div>
                {snapshot.finished && <p className="hint">경기가 종료되었거나 곧 종료됩니다.</p>}
                <button className="secondary" type="button" onClick={() => navigate('/lobby')}>
                  로비로 돌아가기
                </button>
              </div>
            ) : (
              <p>게임 상태를 불러오는 중...</p>
            )}
          </div>
        )}
      </section>
    </main>
  )
}
