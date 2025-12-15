/**
 * [타입] frontend/src/shared/types/tournament.ts
 * 설명:
 *   - 토너먼트 참가자/매치/상태 응답을 표현하는 TypeScript 타입 정의다.
 *   - v0.7.0 단일 제거 브래킷 UI와 WebSocket 알림에 재사용한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/frontend/v0.7.0-tournament-ui.md
 */
export interface TournamentParticipant {
  id: number
  userId: number
  nickname: string
  seed: number
}

export type TournamentStatus = 'REGISTRATION' | 'IN_PROGRESS' | 'COMPLETED'
export type TournamentMatchStatus = 'PENDING' | 'READY' | 'COMPLETED'

export interface TournamentMatch {
  id: number
  round: number
  position: number
  status: TournamentMatchStatus
  roomId?: string | null
  scoreA?: number | null
  scoreB?: number | null
  participantA?: TournamentParticipant | null
  participantB?: TournamentParticipant | null
  winnerId?: number | null
}

export interface TournamentSummary {
  id: number
  name: string
  creatorId: number
  status: TournamentStatus
  maxParticipants: number
  currentParticipants: number
}

export interface TournamentDetail {
  id: number
  name: string
  creatorId: number
  status: TournamentStatus
  maxParticipants: number
  participants: TournamentParticipant[]
  matches: TournamentMatch[]
}

export interface TournamentEventMessage {
  type: string
  payload: unknown
}
