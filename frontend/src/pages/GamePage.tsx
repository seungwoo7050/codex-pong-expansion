import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthProvider'
import { fetchMatchMessages, sendMatchMessage } from '../features/chat/api'
import { useChatSocket } from '../hooks/useChatSocket'
import { useGameSocket } from '../hooks/useGameSocket'
import { ChatMessage } from '../shared/types/chat'
import { GameCanvas } from '../shared/components/GameCanvas'

/**
 * [페이지] frontend/src/pages/GamePage.tsx
 * 설명:
 *   - WebSocket으로 전달받은 게임 스냅샷을 렌더링하고 간단한 패들 입력 버튼을 제공한다.
 *   - v0.4.0에서는 랭크/일반 구분과 레이팅 변동 메시지를 표시하고, v0.6.0에서는 매치 채팅 패널을 추가한다.
 *   - v0.8.0에서는 관전자 수를 표시해 관전 모드와 동일한 스냅샷 포맷을 공유한다.
 *   - v0.11.0에서는 리플레이 뷰어와 동일한 GameCanvas를 재사용하도록 구조를 정리한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/frontend/v0.8.0-spectator-ui.md
 *   - design/realtime/v0.8.0-spectator-events.md
 *   - design/frontend/v0.11.0-replay-browser-and-viewer.md
 */
export function GamePage() {
  const { token, user } = useAuth()
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const roomId = params.get('roomId')

  const { connected, error, snapshot, sendInput, matchType, ratingChange, spectatorCount } = useGameSocket(
    roomId,
    token,
  )
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([])
  const [chatInput, setChatInput] = useState('')
  const chatSocket = useChatSocket(token, (msg) => {
    if (msg.channelType === 'MATCH' && roomId && msg.channelKey === roomId) {
      setChatMessages((prev) => [...prev, msg])
    }
  })

  useEffect(() => {
    if (!roomId) {
      navigate('/lobby')
    }
  }, [roomId, navigate])

  useEffect(() => {
    if (!roomId) return
    fetchMatchMessages(roomId, token)
      .then((res) => setChatMessages(res.messages))
      .catch(() => {})
    chatSocket.subscribeMatch(roomId)
  }, [roomId, token])

  const ratingMessage = useMemo(() => {
    if (!user || !ratingChange || matchType !== 'RANKED') return ''
    if (ratingChange.winnerId === user.id) {
      return `랭크전 승리! 레이팅 +${ratingChange.winnerDelta}`
    }
    if (ratingChange.loserId === user.id) {
      return `랭크전 패배... 레이팅 ${ratingChange.loserDelta}`
    }
    return '랭크전 결과가 반영되었습니다.'
  }, [matchType, ratingChange, user])

  return (
    <main className="page">
      <section className="panel">
        <h2>실시간 경기</h2>
        <p>
          방 번호: {roomId ?? '없음'} / 연결 상태: {connected ? '연결됨' : '대기 중'} / 타입:{' '}
          {matchType === 'RANKED' ? '랭크' : '일반'} / 관전자: {spectatorCount}
        </p>
        {error && <p className="error">{error}</p>}
        {snapshot ? (
          <div className="game-area">
            <GameCanvas snapshot={snapshot} />
            <div className="controls">
              <button type="button" onClick={() => sendInput('UP')}>
                위로
              </button>
              <button type="button" onClick={() => sendInput('STAY')}>
                정지
              </button>
              <button type="button" onClick={() => sendInput('DOWN')}>
                아래로
              </button>
            </div>
            {snapshot.finished && <p className="hint">경기가 종료되었습니다.</p>}
            {ratingMessage && <p className="success">{ratingMessage}</p>}
            <div className="chat-box">
              <h3>매치 채팅</h3>
              <div className="chat-messages">
                {chatMessages.map((msg) => (
                  <div key={msg.id} className="chat-line">
                    <strong>{msg.senderNickname}</strong>: {msg.content}
                  </div>
                ))}
                {chatMessages.length === 0 && <p className="hint">경기 중 채팅이 없습니다.</p>}
              </div>
              <div className="chat-input">
                <input
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  placeholder="팀원과 대화하기"
                />
                <button
                  className="button"
                  type="button"
                  onClick={() => {
                    if (!roomId || !chatInput.trim()) return
                    chatSocket.sendMatch(roomId, chatInput.trim())
                    if (token) {
                      sendMatchMessage(token, roomId, chatInput.trim())
                    }
                    setChatInput('')
                  }}
                >
                  보내기
                </button>
              </div>
            </div>
          </div>
        ) : (
          <p>게임 상태를 불러오는 중...</p>
        )}
      </section>
    </main>
  )
}
