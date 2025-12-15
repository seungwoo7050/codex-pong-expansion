import fs from 'fs/promises'
import path from 'path'
import { chromium } from 'playwright'

const basePort = process.env.PERF_PORT ?? '8000'
const baseUrl = `http://127.0.0.1:${basePort}`
const outputDir = path.resolve('../reports/performance/v0.14.0')

const defaultUser = { id: 1, username: 'demo', nickname: '프로파일', avatarUrl: null, rating: 2500 }

/**
 * [스크립트] frontend/scripts/captureReflowTrace.js
 * 설명:
 *   - v0.14.0 리플로우 감사 대상 화면(리더보드, 작업 목록)의 트레이스를 저장한다.
 *   - 로컬 dev 서버(기본 8000 포트)에 연결하고 API 응답을 목킹한다.
 * 사용법:
 *   - node frontend/scripts/captureReflowTrace.js
 *   - 환경변수 PERF_PORT로 프런트엔드 dev 포트를 바꿀 수 있다.
 */
async function capture(name, url, mocks) {
  await fs.mkdir(outputDir, { recursive: true })
  const browser = await chromium.launch()
  const context = await browser.newContext()
  await context.addInitScript(() => {
    localStorage.setItem('authToken', 'test-token')
  })

  for (const [pattern, payload] of mocks) {
    await context.route(pattern, (route) =>
      route.fulfill({ status: 200, body: JSON.stringify(payload), headers: { 'Content-Type': 'application/json' } }),
    )
  }
  await context.route('**/*', (route) => route.continue())

  const page = await context.newPage()
  await context.tracing.start({ screenshots: true, snapshots: true })
  await page.goto(url)
  await page.waitForTimeout(2000)
  const tracePath = path.join(outputDir, `${name}.zip`)
  await context.tracing.stop({ path: tracePath })
  await browser.close()
  console.log(`saved ${tracePath}`)
}

async function main() {
  const leaderboardPayload = Array.from({ length: 120 }, (_, idx) => ({
    rank: idx + 1,
    userId: idx + 1,
    nickname: `사용자${idx + 1}`,
    rating: 3000 - idx * 3,
    avatarUrl: null,
  }))
  const liveMatches = Array.from({ length: 10 }, (_, idx) => ({
    roomId: `room${idx}`,
    leftPlayerId: idx + 1,
    rightPlayerId: idx + 2,
  }))

  await capture('leaderboard-after', `${baseUrl}/leaderboard`, [
    ['**/api/users/me', defaultUser],
    ['**/api/rank/leaderboard', leaderboardPayload],
    ['**/api/match/ongoing', liveMatches],
  ])

  const jobs = {
    items: Array.from({ length: 80 }, (_, idx) => ({
      jobId: idx + 1,
      jobType: idx % 2 === 0 ? 'REPLAY_EXPORT_MP4' : 'REPLAY_THUMBNAIL',
      status: idx % 3 === 0 ? 'SUCCEEDED' : 'RUNNING',
      progress: (idx * 7) % 100,
      createdAt: '2024-10-12T12:00:00Z',
      targetReplayId: 1000 + idx,
      downloadUrl: 'https://example.com/file.mp4',
      errorMessage: '',
    })),
    page: 0,
    size: 80,
    totalItems: 80,
    totalPages: 1,
  }

  await capture('jobs-after', `${baseUrl}/jobs`, [
    ['**/api/users/me', defaultUser],
    ['**/api/jobs*', jobs],
  ])
}

main().catch((err) => {
  console.error('capture failed', err)
  process.exit(1)
})
