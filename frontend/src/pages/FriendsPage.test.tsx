import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { FriendsPage } from './FriendsPage'
import { MemoryRouter } from 'react-router-dom'

vi.mock('../features/auth/AuthProvider', () => ({
  useAuth: () => ({
    token: 'test-token',
    status: 'authenticated',
    user: { id: 1, nickname: '테스터' },
    logout: vi.fn(),
  }),
}))

describe('FriendsPage', () => {
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
    vi.spyOn(global, 'fetch').mockImplementation((input, init) => {
      const url = typeof input === 'string' ? input : input.url
      if (url.endsWith('/api/social/friends')) {
        return Promise.resolve(new Response(JSON.stringify([
          { userId: 2, nickname: '친구A', online: true, since: new Date().toISOString() },
        ]), { status: 200 }))
      }
      if (url.endsWith('/api/social/friend-requests')) {
        return Promise.resolve(new Response(JSON.stringify({
          incoming: [{ id: 1, senderId: 3, senderNickname: '요청자', receiverId: 1, status: 'PENDING', createdAt: new Date().toISOString() }],
          outgoing: [],
        }), { status: 200 }))
      }
      if (url.includes('/friend-requests/1/accept')) {
        return Promise.resolve(new Response(JSON.stringify({ id: 1, status: 'ACCEPTED' }), { status: 200 }))
      }
      if (url.endsWith('/api/social/blocks')) {
        return Promise.resolve(new Response(JSON.stringify([]), { status: 200 }))
      }
      if (url.endsWith('/api/social/invites/incoming') || url.endsWith('/api/social/invites/outgoing')) {
        return Promise.resolve(new Response(JSON.stringify([]), { status: 200 }))
      }
      if (url.endsWith('/api/match/ongoing')) {
        return Promise.resolve(new Response(JSON.stringify([]), { status: 200 }))
      }
      if (url.includes('/api/chat/dm') || url.includes('/api/chat/lobby')) {
        return Promise.resolve(new Response(JSON.stringify({ messages: [] }), { status: 200 }))
      }
      return Promise.resolve(new Response(JSON.stringify({}), { status: 200 }))
    })
  })

  it('친구 목록과 요청을 렌더링하고 수락 요청을 보낸다', async () => {
    const queryClient = new QueryClient()
    render(
      <MemoryRouter>
        <QueryClientProvider client={queryClient}>
          <FriendsPage />
        </QueryClientProvider>
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('친구A')).toBeInTheDocument()
    })

    expect(screen.getByText('요청자')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: '수락' }))

    await waitFor(() => {
      const fetchMock = vi.mocked(global.fetch)
      expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/friend-requests/1/accept'), expect.anything())
    })
  })
})
