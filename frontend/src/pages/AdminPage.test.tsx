import { render, screen, waitFor } from '@testing-library/react'
import { vi } from 'vitest'
import { AdminPage } from './AdminPage'
import { useAuth } from '../features/auth/AuthProvider'
import * as adminApi from '../shared/api/admin'

vi.mock('../features/auth/AuthProvider', () => ({
  useAuth: vi.fn(),
}))
vi.mock('../shared/api/admin')

const mockAuth = useAuth as unknown as vi.Mock

/**
 * [테스트] frontend/src/pages/AdminPage.test.tsx
 * 설명:
 *   - 관리자 콘솔 초기 렌더링 시 통계와 사용자 테이블이 노출되는지 확인한다.
 */
describe('AdminPage', () => {
  beforeEach(() => {
    mockAuth.mockReturnValue({ token: 'test', status: 'authenticated', logout: vi.fn() })
    vi.spyOn(adminApi, 'fetchAdminUsers').mockResolvedValue([
      {
        id: 1,
        username: 'user1',
        nickname: '테스터',
        rating: 1200,
        banned: false,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    ] as any)
    vi.spyOn(adminApi, 'fetchAdminMatches').mockResolvedValue([] as any)
    vi.spyOn(adminApi, 'fetchAdminStats').mockResolvedValue({
      userCount: 1,
      totalMatches: 0,
      activeGames: 0,
      activeSpectators: 0,
    })
    vi.spyOn(adminApi, 'fetchAdminUserMatches').mockResolvedValue([] as any)
    vi.spyOn(adminApi, 'applyModeration').mockResolvedValue({
      id: 1,
      username: 'user1',
      nickname: '테스터',
      rating: 1200,
      banned: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    } as any)
  })

  it('renders stats and user table', async () => {
    render(<AdminPage />)

    await waitFor(() => {
      expect(screen.getByText('운영자 콘솔')).toBeInTheDocument()
      expect(screen.getByText(/등록 사용자/)).toBeInTheDocument()
      expect(screen.getByText('테스터')).toBeInTheDocument()
    })
  })
})
