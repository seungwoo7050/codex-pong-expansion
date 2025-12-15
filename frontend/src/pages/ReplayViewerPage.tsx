import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthProvider'
import { fetchReplayDetail, fetchReplayEvents } from '../features/replay/api'
import { requestMp4Export, requestThumbnailExport, fetchJob } from '../features/jobs/api'
import { ReplayRenderer } from '../shared/components/ReplayRenderer'
import { JobDrawer } from '../shared/components/JobDrawer'
import { JobSummary } from '../shared/types/job'
import { ReplayDetail, ReplayEventRecord } from '../shared/types/replay'
import { useJobSocket, JobSocketEvent } from '../hooks/useJobSocket'

/**
 * [페이지] frontend/src/pages/ReplayViewerPage.tsx
 * 설명:
 *   - v0.13.0 리플레이 뷰어는 WebGL → Canvas2D 자동 폴백 렌더러와 GPU/CPU 겸용 내보내기 트리거 UI를 제공한다.
 *   - 개발 모드에서는 1x/2x 평균 FPS를 기록해 GPU 경로 확인에 활용한다.
 * 버전: v0.13.0
 * 관련 설계문서:
 *   - design/frontend/v0.11.0-replay-browser-and-viewer.md
 *   - design/frontend/v0.12.0-replay-export-and-jobs-ui.md
 *   - design/frontend/v0.13.0-webgl-replay-renderer.md
 */
export function ReplayViewerPage() {
  const { replayId } = useParams()
  const { token } = useAuth()
  const [detail, setDetail] = useState<ReplayDetail | null>(null)
  const [events, setEvents] = useState<ReplayEventRecord[]>([])
  const [positionMs, setPositionMs] = useState(0)
  const [playing, setPlaying] = useState(false)
  const [speed, setSpeed] = useState(1)
  const [error, setError] = useState('')
  const [jobError, setJobError] = useState('')
  const [activeJob, setActiveJob] = useState<JobSummary | null>(null)
  const [jobLogs, setJobLogs] = useState<string[]>([])
  const [fpsInfo, setFpsInfo] = useState<{ [key: string]: number }>({})
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const fpsStatRef = useRef<{ [key: string]: { start: number; frames: number } }>({})

  const durationMs = detail?.summary.durationMs ?? 0

  const handleJobEvent = useCallback(
    (event: JobSocketEvent) => {
      if (!activeJob) return
      const payload = event.payload as { jobId?: number; progress?: number; phase?: string; message?: string; resultUri?: string; errorMessage?: string }
      if (!payload?.jobId || Number(payload.jobId) !== activeJob.jobId) {
        return
      }
      if (event.type === 'job.progress') {
        setActiveJob((prev) => (prev ? { ...prev, status: 'RUNNING', progress: payload.progress ?? prev.progress } : prev))
        const logLine = `${payload.phase ?? '진행'}: ${payload.message ?? ''} (${payload.progress ?? 0}%)`
        setJobLogs((prev) => [...prev, logLine].slice(-30))
      }
      if (event.type === 'job.completed') {
        setActiveJob((prev) =>
          prev
            ? {
                ...prev,
                status: 'SUCCEEDED',
                progress: 100,
                resultUri: payload.resultUri ?? prev.resultUri,
                downloadUrl: `/api/jobs/${prev.jobId}/result`,
              }
            : prev,
        )
        setJobLogs((prev) => [...prev, '완료: 결과 파일이 준비되었습니다.'].slice(-30))
      }
      if (event.type === 'job.failed') {
        setActiveJob((prev) => (prev ? { ...prev, status: 'FAILED', errorMessage: payload.errorMessage ?? prev.errorMessage } : prev))
        setJobLogs((prev) => [...prev, `실패: ${payload.errorMessage ?? '워커 오류'}`].slice(-30))
      }
    },
    [activeJob],
  )

  useJobSocket(token, handleJobEvent)

  useEffect(() => {
    if (!token || !replayId) return
    fetchReplayDetail(Number(replayId), token)
      .then((data) => {
        setDetail(data)
        setError('')
        return fetchReplayEvents(data.downloadPath, token)
      })
      .then((records) => {
        setEvents(records)
        setPositionMs(0)
      })
      .catch(() => setError('리플레이 정보를 불러오는 중 문제가 발생했습니다.'))
  }, [replayId, token])

  useEffect(() => {
    if (playing) {
      timerRef.current = setInterval(() => {
        setPositionMs((prev) => {
          const next = Math.min(durationMs, prev + 50 * speed)
          if (next >= durationMs) {
            setPlaying(false)
            return durationMs
          }
          return next
        })
      }, 50)
    }
    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
    }
  }, [playing, speed, durationMs])

  useEffect(() => {
    if (!activeJob || !token) return
    fetchJob(activeJob.jobId, token)
      .then((latest) => setActiveJob(latest))
      .catch(() => setJobError('잡 상태를 새로고침하는데 실패했습니다.'))
  }, [activeJob?.jobId, token])

  const currentSnapshot = useMemo(() => {
    if (events.length === 0) return null
    let latest = events[0].snapshot
    for (const event of events) {
      if (event.offsetMs <= positionMs) {
        latest = event.snapshot
      } else {
        break
      }
    }
    return latest
  }, [events, positionMs])

  const togglePlay = () => {
    if (!events.length) return
    setPlaying((prev) => !prev)
  }

  const handleSeek = (value: number) => {
    setPositionMs(value)
    setPlaying(false)
  }

  const startJob = async (type: 'mp4' | 'thumbnail') => {
    if (!token || !replayId) return
    setJobError('')
    setJobLogs([])
    try {
      const created =
        type === 'mp4'
          ? await requestMp4Export(Number(replayId), token)
          : await requestThumbnailExport(Number(replayId), token)
      const job = await fetchJob(created.jobId, token)
      setActiveJob(job)
      setJobLogs([`${type === 'mp4' ? 'MP4' : '썸네일'} 내보내기 잡이 생성되었습니다.`])
    } catch (e) {
      setJobError('잡 생성 요청에 실패했습니다. 잠시 후 다시 시도하세요.')
    }
  }

  const refreshJob = () => {
    if (!activeJob || !token) return
    fetchJob(activeJob.jobId, token)
      .then((job) => setActiveJob(job))
      .catch(() => setJobError('잡 상태를 불러오지 못했습니다.'))
  }

  useEffect(() => {
    if (!playing) return
    if (speed !== 1 && speed !== 2) return
    const key = `${speed}x`
    const now = performance.now()
    const stat = fpsStatRef.current[key] ?? { start: now, frames: 0 }
    if (stat.start === 0) {
      stat.start = now
    }
    stat.frames += 1
    fpsStatRef.current[key] = stat
    const elapsed = now - stat.start
    if (elapsed > 0) {
      const avg = (stat.frames / elapsed) * 1000
      setFpsInfo((prev) => ({ ...prev, [key]: Math.round(avg * 10) / 10 }))
      if (import.meta.env.DEV && stat.frames % 60 === 0) {
        // 개발용 콘솔 로깅으로 GPU 경로를 빠르게 점검한다.
        console.debug(`[replay-fps] ${key}: ${avg.toFixed(1)} fps`)
      }
    }
  }, [playing, positionMs, speed])

  return (
    <main className="page">
      <section className="panel">
        <h2>리플레이 뷰어</h2>
        {detail && (
          <p>
            상대: {detail.summary.opponentNickname} / 결과: {detail.summary.myScore} : {detail.summary.opponentScore} /
            길이: {(detail.summary.durationMs / 1000).toFixed(1)}초
          </p>
        )}
        <div className="actions">
          <button className="button" type="button" onClick={() => startJob('mp4')}>
            MP4 내보내기
          </button>
          <button className="secondary" type="button" onClick={() => startJob('thumbnail')}>
            썸네일 생성
          </button>
          <Link className="secondary" to="/jobs">
            내 잡 목록 보기
          </Link>
        </div>
        {jobError && <p className="error">{jobError}</p>}
        {error && <p className="error">{error}</p>}
        {!currentSnapshot && !error && <p className="hint">이벤트를 불러오는 중...</p>}
        {currentSnapshot && (
          <div className="game-area">
            <ReplayRenderer snapshot={currentSnapshot} />
            <div className="scoreboard">
              <div>왼쪽: {currentSnapshot.leftScore}</div>
              <div>오른쪽: {currentSnapshot.rightScore}</div>
              <div>목표: {currentSnapshot.targetScore}</div>
            </div>
            <div className="controls">
              <button type="button" onClick={togglePlay}>
                {playing ? '일시정지' : '재생'}
              </button>
              <label>
                배속
                <select value={speed} onChange={(e) => setSpeed(Number(e.target.value))}>
                  <option value={0.5}>0.5x</option>
                  <option value={1}>1x</option>
                  <option value={2}>2x</option>
                </select>
              </label>
              <input
                type="range"
                min={0}
                max={durationMs}
                value={positionMs}
                onChange={(e) => handleSeek(Number(e.target.value))}
              />
              <span>
                {Math.round(positionMs / 100) / 10}s / {Math.round(durationMs / 100) / 10}s
              </span>
              <Link className="button" to="/replays">
                목록으로
              </Link>
            </div>
            {import.meta.env.DEV && (
              <div className="hint">
                <p>개발용 FPS 기록: 1x {fpsInfo['1x'] ?? '-'} / 2x {fpsInfo['2x'] ?? '-'}</p>
              </div>
            )}
          </div>
        )}
      </section>
      <JobDrawer job={activeJob} logs={jobLogs} onClose={() => setActiveJob(null)} onRefresh={refreshJob} />
    </main>
  )
}
