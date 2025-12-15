import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthProvider'
import { fetchJobs } from '../features/jobs/api'
import { JobPage, JobStatus, JobType } from '../shared/types/job'

/**
 * [페이지] frontend/src/pages/JobsPage.tsx
 * 설명:
 *   - v0.12.0 리플레이 내보내기 잡 목록을 상태/유형별로 필터링하고 진행률을 확인한다.
 *   - 완료된 항목은 다운로드 링크, 실패 항목은 오류 메시지를 보여준다.
 * 버전: v0.14.0
 * 관련 설계문서:
 *   - design/frontend/v0.12.0-replay-export-and-jobs-ui.md
 *   - design/frontend/v0.14.0-reflow-audit-and-fixes.md
 */
export function JobsPage() {
  const { token } = useAuth()
  const [page, setPage] = useState<JobPage | null>(null)
  const [pageIndex, setPageIndex] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [statusFilter, setStatusFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    if (!token) return
    setLoading(true)
    fetchJobs(
      {
        page: pageIndex,
        size: pageSize,
        status: statusFilter || undefined,
        type: typeFilter || undefined,
      },
      token,
    )
      .then((res) => {
        setPage(res)
        setError('')
      })
      .catch(() => setError('잡 목록을 불러오는 중 오류가 발생했습니다.'))
      .finally(() => setLoading(false))
  }, [token, statusFilter, typeFilter, refreshKey, pageIndex, pageSize])

  useEffect(() => {
    setPageIndex(0)
  }, [statusFilter, typeFilter, pageSize])

  const statusOptions: JobStatus[] = ['QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED']
  const typeOptions: JobType[] = ['REPLAY_EXPORT_MP4', 'REPLAY_THUMBNAIL']

  return (
    <main className="page">
      <section className="panel">
        <h2>내 작업 목록</h2>
        <p className="muted">실행 중인 내보내기 잡의 상태와 다운로드 가능 여부를 확인할 수 있습니다.</p>
        <div className="inline-form">
          <label>
            상태 필터
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="">전체</option>
              {statusOptions.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>
          <label>
            유형 필터
            <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
              <option value="">전체</option>
              {typeOptions.map((t) => (
                <option key={t} value={t}>
                  {t === 'REPLAY_EXPORT_MP4' ? 'MP4' : '썸네일'}
                </option>
              ))}
            </select>
          </label>
          <button type="button" className="secondary" onClick={() => setRefreshKey((prev) => prev + 1)}>
            새로고침
          </button>
          <label>
            페이지 크기
            <select value={pageSize} onChange={(e) => setPageSize(Number(e.target.value))}>
              {[10, 20, 50].map((size) => (
                <option key={size} value={size}>
                  {size}개씩 보기
                </option>
              ))}
            </select>
          </label>
        </div>
        {error && <p className="error">{error}</p>}
        {loading && <p>불러오는 중...</p>}
        {page && page.items.length === 0 && <p className="hint">등록된 작업이 없습니다.</p>}
        {page && page.items.length > 0 && (
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>유형</th>
                <th>상태</th>
                <th>진행률</th>
                <th>생성일</th>
                <th>동작</th>
              </tr>
            </thead>
            <tbody>
              {page.items.map((job) => (
                <tr key={job.jobId}>
                  <td>{job.jobId}</td>
                  <td>{job.jobType === 'REPLAY_EXPORT_MP4' ? 'MP4' : '썸네일'}</td>
                  <td>{job.status}</td>
                  <td>
                    <progress max={100} value={job.progress} /> {job.progress}%
                  </td>
                  <td>{new Date(job.createdAt).toLocaleString('ko-KR')}</td>
                  <td>
                    <div className="inline-form">
                      <Link className="secondary" to={`/replays/${job.targetReplayId}`}>
                        리플레이 보기
                      </Link>
                      {job.downloadUrl && job.status === 'SUCCEEDED' && (
                        <a className="button" href={job.downloadUrl}>
                          다운로드
                        </a>
                      )}
                      {job.status === 'FAILED' && <span className="error">{job.errorMessage}</span>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {page && page.totalPages > 0 && (
          <div className="pager" aria-label="작업 목록 페이지 네비게이션">
            <button type="button" className="secondary" disabled={pageIndex === 0} onClick={() => setPageIndex((prev) => Math.max(0, prev - 1))}>
              이전
            </button>
            <span>
              {pageIndex + 1} / {page.totalPages} (총 {page.totalItems}건)
            </span>
            <button
              type="button"
              className="secondary"
              disabled={pageIndex + 1 >= page.totalPages}
              onClick={() => setPageIndex((prev) => Math.min(page.totalPages - 1, prev + 1))}
            >
              다음
            </button>
          </div>
        )}
      </section>
    </main>
  )
}
