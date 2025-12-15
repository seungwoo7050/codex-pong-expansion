/**
 * [타입] frontend/src/shared/types/user.ts
 * 설명:
 *   - 인증/프로필 API에서 공통으로 사용하는 사용자 프로필 타입 정의.
 *   - v0.4.0에서 레이팅 정보를 포함해 랭크/리더보드 UI에서 재사용한다.
 * 버전: v0.10.0
 * 관련 설계문서:
 *   - design/frontend/v0.10.0-kor-ux.md
 */
export interface UserProfile {
  id: number
  username: string
  nickname: string
  avatarUrl?: string | null
  rating: number
  authProvider?: string | null
  locale?: string | null
  createdAt: string
  updatedAt: string
}
