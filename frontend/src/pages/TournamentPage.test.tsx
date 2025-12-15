import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TournamentPage } from './TournamentPage'

vi.mock('../features/auth/AuthProvider', () => ({
  useAuth: () => ({
    token: 'token',
    status: 'authenticated',
    user: { id: 1, nickname: '호스트' },
    logout: vi.fn(),
  }),
}))

describe('TournamentPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    const socketMock = {
      send: vi.fn(),
      close: vi.fn(),
      readyState: WebSocket.OPEN,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      set onopen(handler) {
        if (typeof handler === 'function') handler({} as Event)
      },
      set onclose(_handler) {},
      set onerror(_handler) {},
      set onmessage(_handler) {},
    } as unknown as WebSocket
    vi.stubGlobal('WebSocket', vi.fn(() => socketMock))

    const baseDetail = {
      id: 1,
      name: '주말 이벤트',
      creatorId: 1,
      status: 'REGISTRATION',
      maxParticipants: 4,
      participants: [{ id: 1, userId: 1, nickname: '호스트', seed: 1 }],
      matches: [],
    }
    let currentDetail = { ...baseDetail }
    const listState = () => [
      {
        id: currentDetail.id,
        name: currentDetail.name,
        creatorId: currentDetail.creatorId,
        status: currentDetail.status,
        maxParticipants: currentDetail.maxParticipants,
        currentParticipants: currentDetail.participants.length,
      },
    ]

    vi.spyOn(global, 'fetch').mockImplementation((input, init) => {
      const url = typeof input === 'string' ? input : input.url
      const method = init?.method ?? 'GET'
      if (url.endsWith('/api/tournaments') && method === 'GET') {
        return Promise.resolve(new Response(JSON.stringify(listState()), { status: 200 }))
      }
      if (url.endsWith('/api/tournaments') && method === 'POST') {
        currentDetail = { ...baseDetail, name: JSON.parse(init?.body as string).name }
        return Promise.resolve(new Response(JSON.stringify(currentDetail), { status: 201 }))
      }
      if (url.endsWith('/api/tournaments/1') && method === 'GET') {
        return Promise.resolve(new Response(JSON.stringify(currentDetail), { status: 200 }))
      }
      if (url.endsWith('/api/tournaments/1/join')) {
        currentDetail = {
          ...currentDetail,
          participants: [
            ...currentDetail.participants,
            { id: 2, userId: 2, nickname: '참가자', seed: 2 },
          ],
        }
        return Promise.resolve(new Response(JSON.stringify(currentDetail), { status: 200 }))
      }
      if (url.endsWith('/api/tournaments/1/start')) {
        currentDetail = {
          ...currentDetail,
          status: 'IN_PROGRESS',
          matches: [
            {
              id: 10,
              round: 1,
              position: 0,
              status: 'READY',
              roomId: 'room-1',
              scoreA: null,
              scoreB: null,
              participantA: currentDetail.participants[0],
              participantB: currentDetail.participants[1],
              winnerId: null,
            },
          ],
        }
        return Promise.resolve(new Response(JSON.stringify(currentDetail), { status: 200 }))
      }
      return Promise.resolve(new Response(JSON.stringify({}), { status: 200 }))
    })
  })

  it('토너먼트를 생성하고 브래킷을 표시한다', async () => {
    render(
      <MemoryRouter>
        <TournamentPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('토너먼트 목록')).toBeInTheDocument()
    })

    await userEvent.type(screen.getByPlaceholderText('토너먼트 이름'), '주말 이벤트')
    await userEvent.click(screen.getByRole('button', { name: '생성' }))

    await waitFor(() => {
      expect(screen.getByText('주말 이벤트 브래킷')).toBeInTheDocument()
    })

    await userEvent.click(screen.getByRole('button', { name: '참여' }))
    await userEvent.click(screen.getByRole('button', { name: '시작' }))

    await waitFor(() => {
      expect(screen.getByText(/roomId/)).toBeInTheDocument()
    })
  })
})
