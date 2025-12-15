/**
 * [타입] frontend/src/shared/types/match.ts
 * 설명:
 *   - 경기 결과 응답을 프런트엔드에서 공통으로 재사용하기 위한 타입 정의다.
 *   - v0.9.0 관리자 콘솔에서도 동일한 필드를 활용한다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/frontend/v0.9.0-admin-console.md
 */
export interface MatchResult {
  id: number
  playerAId: number
  playerANickname: string
  playerBId: number
  playerBNickname: string
  scoreA: number
  scoreB: number
  matchType: 'NORMAL' | 'RANKED'
  ratingChangeA: number
  ratingChangeB: number
  ratingAfterA: number
  ratingAfterB: number
  roomId: string
  startedAt: string
  finishedAt: string
}
