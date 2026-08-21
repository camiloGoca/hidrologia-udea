import { beforeEach, describe, expect, it, vi } from 'vitest'

import { requireAdmin } from '@/router/adminGuard'
import { getAdminMe } from '@/services/api/adminService'
import { observeAuthState, signOut } from '@/services/firebase/authService'

vi.mock('@/services/api/adminService', () => ({
  getAdminMe: vi.fn<() => Promise<{ authenticated: boolean; role: 'ADMIN' }>>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  observeAuthState: vi.fn<(callback: (user: unknown) => void) => () => void>(),
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetAdminMe = vi.mocked(getAdminMe)
const mockedObserveAuthState = vi.mocked(observeAuthState)
const mockedSignOut = vi.mocked(signOut)

describe('adminGuard', () => {
  beforeEach(() => {
    mockedGetAdminMe.mockReset()
    mockedObserveAuthState.mockReset()
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('redirects to login when Firebase has no current user', async () => {
    mockAuthState(null)

    await expect(requireAdmin()).resolves.toEqual({ name: 'admin-login' })
    expect(mockedGetAdminMe).not.toHaveBeenCalled()
  })

  it('allows navigation when Firebase user is authorized by backend', async () => {
    mockAuthState({ uid: 'admin-uid' })
    mockedGetAdminMe.mockResolvedValue({ authenticated: true, role: 'ADMIN' })

    await expect(requireAdmin()).resolves.toBe(true)
  })

  it('signs out and redirects when backend rejects the user', async () => {
    mockAuthState({ uid: 'other-uid' })
    mockedGetAdminMe.mockRejectedValue(new Error('Forbidden'))

    await expect(requireAdmin()).resolves.toEqual({
      name: 'admin-login',
      query: { reason: 'forbidden' },
    })
    expect(mockedSignOut).toHaveBeenCalled()
  })
})

function mockAuthState(user: unknown) {
  mockedObserveAuthState.mockImplementation((callback) => {
    callback(user as never)

    return vi.fn<() => void>()
  })
}
