import { describe, expect, it, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { JobsPage } from './JobsPage'

vi.mock('../features/auth/AuthProvider', () => ({
  useAuth: () => ({
    token: 'token',
    status: 'authenticated',
    user: { id: 1, nickname: '플레이어' },
    logout: vi.fn(),
  }),
}))

describe('JobsPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.spyOn(global, 'fetch').mockImplementation((input) => {
      const url = typeof input === 'string' ? input : input.url
      if (url.includes('/api/jobs')) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              items: [
                {
                  jobId: 1,
                  jobType: 'REPLAY_EXPORT_MP4',
                  status: 'SUCCEEDED',
                  progress: 100,
                  targetReplayId: 10,
                  createdAt: '2024-01-01T10:00:00+09:00',
                  startedAt: '2024-01-01T10:00:05+09:00',
                  endedAt: '2024-01-01T10:00:20+09:00',
                  downloadUrl: '/api/jobs/1/result',
                },
              ],
              page: 0,
              size: 20,
              totalItems: 1,
              totalPages: 1,
            }),
            { status: 200 },
          ),
        )
      }
      return Promise.resolve(new Response('{}', { status: 200 }))
    })
  })

  it('잡 목록을 렌더링하고 다운로드 링크를 노출한다', async () => {
    render(
      <MemoryRouter>
        <JobsPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('내 작업 목록')).toBeInTheDocument()
      expect(screen.getByText('MP4')).toBeInTheDocument()
    })
    expect(screen.getByText('다운로드')).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText('상태 필터'), 'SUCCEEDED')
    expect(screen.getByDisplayValue('SUCCEEDED')).toBeInTheDocument()
  })
})
