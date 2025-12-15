import { apiFetch } from './client'
import { TournamentDetail, TournamentSummary } from '../types/tournament'

/**
 * [API] frontend/src/shared/api/tournament.ts
 * 설명:
 *   - 토너먼트 생성/참여/조회 REST 호출을 캡슐화한다.
 *   - JWT 토큰을 전달해 보호된 엔드포인트에 접근한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/frontend/v0.7.0-tournament-ui.md
 */
export async function fetchTournaments(token?: string | null) {
  return apiFetch<TournamentSummary[]>('/api/tournaments', { method: 'GET' }, token)
}

export async function fetchTournamentDetail(id: number, token?: string | null) {
  return apiFetch<TournamentDetail>(`/api/tournaments/${id}`, { method: 'GET' }, token)
}

export async function createTournament(name: string, maxParticipants: number, token?: string | null) {
  return apiFetch<TournamentDetail>(
    '/api/tournaments',
    {
      method: 'POST',
      body: JSON.stringify({ name, maxParticipants }),
    },
    token,
  )
}

export async function joinTournament(id: number, token?: string | null) {
  return apiFetch<TournamentDetail>(`/api/tournaments/${id}/join`, { method: 'POST' }, token)
}

export async function startTournament(id: number, token?: string | null) {
  return apiFetch<TournamentDetail>(`/api/tournaments/${id}/start`, { method: 'POST' }, token)
}
