import { describe, expect, it, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MyReplaysPage } from './MyReplaysPage'

vi.mock('../features/auth/AuthProvider', () => ({
  useAuth: () => ({
    token: 'token',
    status: 'authenticated',
    user: { id: 1, nickname: '플레이어' },
    logout: vi.fn(),
  }),
}))

describe('MyReplaysPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.spyOn(global, 'fetch').mockImplementation((input) => {
      const url = typeof input === 'string' ? input : input.url
      if (url.includes('/api/replays')) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              items: [
                {
                  replayId: 1,
                  matchId: 10,
                  ownerUserId: 1,
                  opponentUserId: 2,
                  opponentNickname: '상대A',
                  matchType: 'NORMAL',
                  myScore: 5,
                  opponentScore: 3,
                  durationMs: 42000,
                  createdAt: '2024-01-01T10:00:00+09:00',
                  eventFormat: 'JSONL_V1',
                },
                {
                  replayId: 2,
                  matchId: 11,
                  ownerUserId: 1,
                  opponentUserId: 3,
                  opponentNickname: '친구B',
                  matchType: 'RANKED',
                  myScore: 2,
                  opponentScore: 5,
                  durationMs: 30000,
                  createdAt: '2024-01-02T10:00:00+09:00',
                  eventFormat: 'JSONL_V1',
                },
              ],
              page: 0,
              size: 20,
              totalElements: 2,
              totalPages: 1,
            }),
            { status: 200 },
          ),
        )
      }
      return Promise.resolve(new Response('{}', { status: 200 }))
    })
  })

  it('리플레이 목록을 불러오고 닉네임으로 필터링한다', async () => {
    render(
      <MemoryRouter>
        <MyReplaysPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('내 리플레이')).toBeInTheDocument()
    })
    expect(screen.getByText('상대A')).toBeInTheDocument()
    expect(screen.getByText('친구B')).toBeInTheDocument()

    await userEvent.type(screen.getByPlaceholderText('상대 닉네임 검색'), '상대A')
    expect(screen.getByText('상대A')).toBeInTheDocument()
    expect(screen.queryByText('친구B')).not.toBeInTheDocument()
  })
})
