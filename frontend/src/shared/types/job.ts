/**
 * [타입] frontend/src/shared/types/job.ts
 * 설명:
 *   - v0.12.0 리플레이 내보내기 잡 상태/응답 모델을 정의한다.
 *   - WebSocket 알림과 REST 목록/단건 조회가 동일한 토큰을 사용하도록 고정한다.
 * 버전: v0.14.0
 * 관련 설계문서:
 *   - design/frontend/v0.12.0-replay-export-and-jobs-ui.md
 *   - design/frontend/v0.14.0-reflow-audit-and-fixes.md
 */
export type JobStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED'

export type JobType = 'REPLAY_EXPORT_MP4' | 'REPLAY_THUMBNAIL'

export interface JobSummary {
  jobId: number
  jobType: JobType
  status: JobStatus
  progress: number
  targetReplayId: number
  createdAt: string
  startedAt?: string | null
  endedAt?: string | null
  errorCode?: string | null
  errorMessage?: string | null
  resultUri?: string | null
  downloadUrl?: string | null
}

export interface JobPage {
  items: JobSummary[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}
