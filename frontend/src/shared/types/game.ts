/**
 * [타입] frontend/src/shared/types/game.ts
 * 설명:
 *   - v0.4.0 랭크/일반 구분과 레이팅 변동 정보를 포함하는 실시간 게임 타입 정의다.
 *   - v0.8.0에서는 관전자 역할 구분, 관전자 수 정보를 포함한다.
 * 관련 설계문서:
 *   - design/frontend/v0.8.0-spectator-ui.md
 *   - design/realtime/v0.8.0-spectator-events.md
 */
export interface GameSnapshot {
  roomId: string
  ballX: number
  ballY: number
  ballVelocityX: number
  ballVelocityY: number
  leftPaddleY: number
  rightPaddleY: number
  leftScore: number
  rightScore: number
  targetScore: number
  finished: boolean
}

export interface GameServerMessage {
  type: 'READY' | 'STATE' | 'FINISHED'
  snapshot: GameSnapshot
  matchType: 'NORMAL' | 'RANKED'
  ratingChange?: RatingChange | null
  audienceRole: 'PLAYER' | 'SPECTATOR'
  spectatorCount: number
}

export interface RatingChange {
  winnerId: number | null
  winnerDelta: number
  loserId: number | null
  loserDelta: number
}

export interface LiveMatchSummary {
  roomId: string
  matchType: 'NORMAL' | 'RANKED'
  leftPlayerId: number
  leftNickname: string
  rightPlayerId: number
  rightNickname: string
  startedAt: string | null
  spectatorCount: number
  spectatorLimit: number
}
