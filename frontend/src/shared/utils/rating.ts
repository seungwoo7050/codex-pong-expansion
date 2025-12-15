/**
 * [유틸] frontend/src/shared/utils/rating.ts
 * 설명:
 *   - 레이팅 점수에 따른 티어명을 계산해 UI에 노출한다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/frontend/v0.4.0-ranking-and-leaderboard-ui.md
 */
export function toTierLabel(rating: number | undefined | null): string {
  if (!rating && rating !== 0) return '언랭크'
  if (rating >= 1800) return '플래티넘'
  if (rating >= 1600) return '골드'
  if (rating >= 1400) return '실버'
  if (rating >= 1200) return '브론즈'
  return '비기너'
}
