import { GameSnapshot } from './game'

/**
 * [타입] frontend/src/shared/types/replay.ts
 * 설명:
 *   - v0.11.0 리플레이 목록/상세/이벤트 재생에 필요한 타입 정의다.
 *   - eventFormat은 백엔드 JSONL_V1 고정 포맷을 가리킨다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/frontend/v0.11.0-replay-browser-and-viewer.md
 */
export interface ReplaySummary {
  replayId: number
  matchId: number
  ownerUserId: number
  opponentUserId: number
  opponentNickname: string
  matchType: 'NORMAL' | 'RANKED'
  myScore: number
  opponentScore: number
  durationMs: number
  createdAt: string
  eventFormat: string
}

export interface ReplayDetail {
  summary: ReplaySummary
  checksum: string
  downloadPath: string
}

export interface ReplayPage {
  items: ReplaySummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ReplayEventRecord {
  offsetMs: number
  snapshot: GameSnapshot
}
