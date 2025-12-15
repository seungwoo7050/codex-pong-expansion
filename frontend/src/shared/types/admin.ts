/**
 * [타입] frontend/src/shared/types/admin.ts
 * 설명:
 *   - 관리자 API 응답을 표현하기 위한 타입 묶음이다.
 *   - v0.9.0 운영 콘솔에서 사용자 목록, 제재 결과, 시스템 통계를 표시한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/frontend/v0.10.0-kor-ux.md
 */
export interface AdminUser {
  id: number
  username: string
  nickname: string
  rating: number
  banned: boolean
  banReason?: string | null
  bannedAt?: string | null
  suspendedUntil?: string | null
  mutedUntil?: string | null
  authProvider?: string | null
  locale?: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminStats {
  userCount: number
  totalMatches: number
  activeGames: number
  activeSpectators: number
}

export type ModerationAction = 'BAN' | 'SUSPEND' | 'MUTE'
