import { JobSummary } from '../types/job'

/**
 * [컴포넌트] frontend/src/shared/components/JobDrawer.tsx
 * 설명:
 *   - 리플레이 내보내기 잡의 상태/로그를 표시하고 다운로드 링크를 노출한다.
 *   - 페이지 우측에 고정된 패널 형태로 열고 닫을 수 있다.
 * 버전: v0.12.0
 * 관련 설계문서:
 *   - design/frontend/v0.12.0-replay-export-and-jobs-ui.md
 */
interface JobDrawerProps {
  job: JobSummary | null
  logs: string[]
  onClose: () => void
  onRefresh: () => void
}

export function JobDrawer({ job, logs, onClose, onRefresh }: JobDrawerProps) {
  if (!job) return null

  return (
    <aside className="job-drawer">
      <div className="job-drawer__header">
        <div>
          <p className="muted">잡 ID {job.jobId}</p>
          <h3>{job.jobType === 'REPLAY_EXPORT_MP4' ? 'MP4 내보내기' : '썸네일 생성'}</h3>
          <p className="muted">상태: {job.status}</p>
        </div>
        <button type="button" className="link-button" onClick={onClose}>
          닫기
        </button>
      </div>
      <div className="job-drawer__body">
        <label className="progress-label">
          진행률 {job.progress}%
          <progress max={100} value={job.progress} />
        </label>
        {job.downloadUrl && job.status === 'SUCCEEDED' && (
          <a className="button" href={job.downloadUrl}>
            결과 다운로드
          </a>
        )}
        {job.status === 'FAILED' && (
          <p className="error">{job.errorMessage ?? '알 수 없는 오류가 발생했습니다.'}</p>
        )}
        <div className="job-logs">
          <div className="job-logs__header">
            <span>로그</span>
            <button type="button" className="link-button" onClick={onRefresh}>
              새로고침
            </button>
          </div>
          <ul>
            {logs.length === 0 && <li className="muted">아직 수신된 로그가 없습니다.</li>}
            {logs.map((log, idx) => (
              <li key={`${log}-${idx}`}>{log}</li>
            ))}
          </ul>
        </div>
      </div>
    </aside>
  )
}
