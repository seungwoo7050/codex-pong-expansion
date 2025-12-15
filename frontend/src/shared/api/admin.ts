import { apiFetch } from './client'
import { AdminStats, AdminUser, ModerationAction } from '../types/admin'
import { MatchResult } from '../types/match'

/**
 * [API 클라이언트] frontend/src/shared/api/admin.ts
 * 설명:
 *   - 관리자용 조회/제재 API 호출을 래핑한다.
 *   - v0.9.0 운영 콘솔에서 재사용한다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/frontend/v0.9.0-admin-console.md
 */
export async function fetchAdminUsers(token: string): Promise<AdminUser[]> {
  return apiFetch<AdminUser[]>('/api/admin/users', { method: 'GET' }, token)
}

export async function fetchAdminMatches(token: string): Promise<MatchResult[]> {
  return apiFetch<MatchResult[]>('/api/admin/matches', { method: 'GET' }, token)
}

export async function fetchAdminUserMatches(userId: number, token: string): Promise<MatchResult[]> {
  return apiFetch<MatchResult[]>(`/api/admin/users/${userId}/matches`, { method: 'GET' }, token)
}

export async function applyModeration(
  userId: number,
  action: ModerationAction,
  reason: string,
  durationMinutes: number,
  token: string,
): Promise<AdminUser> {
  return apiFetch<AdminUser>(
    `/api/admin/users/${userId}/moderations`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action, reason, durationMinutes }),
    },
    token,
  )
}

export async function fetchAdminStats(token: string): Promise<AdminStats> {
  return apiFetch<AdminStats>('/api/admin/stats', { method: 'GET' }, token)
}
