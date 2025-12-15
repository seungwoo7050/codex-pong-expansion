import { apiFetch } from '../../shared/api/client'
import { LiveMatchSummary } from '../../shared/types/game'

/**
 * [API 모듈] frontend/src/features/game/api.ts
 * 설명:
 *   - 진행 중인 경기 목록을 가져와 관전 진입에 필요한 roomId를 제공한다.
 * 버전: v0.8.0
 * 관련 설계문서:
 *   - design/frontend/v0.8.0-spectator-ui.md
 */
export function fetchLiveMatches(token: string) {
  return apiFetch<LiveMatchSummary[]>('/api/match/ongoing', { method: 'GET' }, token)
}
