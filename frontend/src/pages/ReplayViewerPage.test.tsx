import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { ReplayViewerPage } from './ReplayViewerPage'

vi.mock('../features/auth/AuthProvider', () => ({
  useAuth: () => ({
    token: 'token',
    status: 'authenticated',
    user: { id: 1, nickname: '플레이어' },
    logout: vi.fn(),
  }),
}))

describe('ReplayViewerPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.spyOn(global, 'fetch').mockImplementation((input) => {
      const url = typeof input === 'string' ? input : input.url
      if (url.endsWith('/api/replays/10')) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              summary: {
                replayId: 10,
                matchId: 99,
                ownerUserId: 1,
                opponentUserId: 2,
                opponentNickname: '상대',
                matchType: 'NORMAL',
                myScore: 1,
                opponentScore: 0,
                durationMs: 5000,
                createdAt: '2024-01-01T10:00:00+09:00',
                eventFormat: 'JSONL_V1',
              },
              checksum: 'abc',
              downloadPath: '/api/replays/10/events',
            }),
            { status: 200 },
          ),
        )
      }
      if (url.endsWith('/api/replays/10/events')) {
        const payload = `${JSON.stringify({ offsetMs: 0, snapshot: { roomId: 'room', ballX: 0, ballY: 0, ballVelocityX: 0, ballVelocityY: 0, leftPaddleY: 10, rightPaddleY: 20, leftScore: 0, rightScore: 0, targetScore: 5, finished: false } })}\n${JSON.stringify({ offsetMs: 3000, snapshot: { roomId: 'room', ballX: 10, ballY: 15, ballVelocityX: 0, ballVelocityY: 0, leftPaddleY: 30, rightPaddleY: 40, leftScore: 3, rightScore: 1, targetScore: 5, finished: true } })}`
        return Promise.resolve(new Response(payload, { status: 200 }))
      }
      return Promise.resolve(new Response('{}', { status: 200 }))
    })
  })

  it('이벤트 스트림을 불러와 재생 제어를 제공한다', async () => {
    render(
      <MemoryRouter initialEntries={["/replays/10"]}>
        <Routes>
          <Route path="/replays/:replayId" element={<ReplayViewerPage />} />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText(/리플레이 뷰어/)).toBeInTheDocument()
      expect(screen.getByText(/상대:/)).toBeInTheDocument()
    })
    expect(screen.getByText(/왼쪽: 0/)).toBeInTheDocument()

    const slider = screen.getByRole('slider') as HTMLInputElement
    fireEvent.change(slider, { target: { value: '3000' } })
    expect(screen.getByText(/왼쪽: 3/)).toBeInTheDocument()

    await waitFor(() => expect(screen.getByRole('button', { name: '재생' })).toBeInTheDocument())
  })
})
