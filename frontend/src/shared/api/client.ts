import { API_BASE_URL } from '../../constants'

/**
 * [유틸] frontend/src/shared/api/client.ts
 * 설명:
 *   - 백엔드 REST API 호출 시 기본 옵션과 에러 처리를 공통화한다.
 *   - Authorization 헤더를 주입해 JWT 기반 인증 요청을 단순화한다.
 * 버전: v0.2.0
 * 관련 설계문서:
 *   - design/frontend/v0.2.0-auth-and-profile-ui.md
 */
export class ApiError extends Error {
  status: number
  body?: unknown

  constructor(status: number, message: string, body?: unknown) {
    super(message)
    this.status = status
    this.body = body
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null,
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  })

  const data = await response.json().catch(() => undefined)

  if (!response.ok) {
    throw new ApiError(response.status, 'API 요청이 실패했습니다.', data)
  }

  return data as T
}
