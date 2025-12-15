import { apiFetch } from '../../shared/api/client'
import { JobPage, JobSummary } from '../../shared/types/job'

/**
 * [API 모듈] frontend/src/features/jobs/api.ts
 * 설명:
 *   - v0.12.0 잡 생성/조회/다운로드 관련 REST 호출을 감싼다.
 *   - 리플레이 뷰어와 잡 목록 화면에서 공통으로 사용한다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/frontend/v0.12.0-replay-export-and-jobs-ui.md
 */
export function requestMp4Export(replayId: number, token: string) {
  return apiFetch<{ jobId: number }>(`/api/replays/${replayId}/exports/mp4`, { method: 'POST' }, token)
}

export function requestThumbnailExport(replayId: number, token: string) {
  return apiFetch<{ jobId: number }>(`/api/replays/${replayId}/exports/thumbnail`, { method: 'POST' }, token)
}

export function fetchJob(jobId: number, token: string) {
  return apiFetch<JobSummary>(`/api/jobs/${jobId}`, { method: 'GET' }, token)
}

export function fetchJobs(params: { page?: number; size?: number; status?: string; type?: string }, token: string) {
  const query = new URLSearchParams()
  if (params.page !== undefined) query.set('page', params.page.toString())
  if (params.size !== undefined) query.set('size', params.size.toString())
  if (params.status) query.set('status', params.status)
  if (params.type) query.set('type', params.type)
  return apiFetch<JobPage>(`/api/jobs?${query.toString()}`, { method: 'GET' }, token)
}
