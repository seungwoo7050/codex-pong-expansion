import { apiFetch } from '../../shared/api/client'
import { UserProfile } from '../../shared/types/user'

/**
 * [API 모듈] frontend/src/features/auth/api.ts
 * 설명:
 *   - 인증 및 프로필 관련 REST 호출을 담당한다.
 *   - JWT 발급과 프로필 조회를 단순화한 헬퍼 함수 모음이다.
 * 버전: v0.4.0
 * 관련 설계문서:
 *   - design/frontend/v0.4.0-ranking-and-leaderboard-ui.md
 */
export interface AuthResponse {
  token: string
  expiresAt: string
  user: UserProfile
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  nickname: string
  avatarUrl?: string
}

export type OAuthProvider = 'kakao' | 'naver'

export function register(payload: RegisterPayload) {
  return apiFetch<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function login(payload: LoginPayload) {
  return apiFetch<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function loginWithProvider(provider: OAuthProvider, accessToken: string) {
  return apiFetch<AuthResponse>(`/api/auth/oauth/${provider}`, {
    method: 'POST',
    body: JSON.stringify({ accessToken }),
  })
}

export function logout(token?: string | null) {
  return apiFetch<{ message: string }>('/api/auth/logout', {
    method: 'POST',
  }, token)
}

export function fetchProfile(token?: string | null) {
  return apiFetch<UserProfile>('/api/users/me', {
    method: 'GET',
  }, token)
}

export function updateProfile(token: string, payload: { nickname: string; avatarUrl?: string }) {
  return apiFetch<UserProfile>('/api/users/me', {
    method: 'PUT',
    body: JSON.stringify(payload),
  }, token)
}
