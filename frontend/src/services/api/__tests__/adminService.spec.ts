import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { adminHttpClient } from '@/services/api/adminHttpClient'
import { getAdminMe } from '@/services/api/adminService'
import type { AdminMeResponse } from '@/types/admin'

vi.mock('@/services/api/adminHttpClient', () => ({
  adminHttpClient: {
    get: vi.fn<(url: string) => Promise<AxiosResponse<AdminMeResponse>>>(),
  },
}))

const mockedGet = vi.mocked(adminHttpClient.get)

describe('adminService', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  it('loads the authorized administrator session', async () => {
    const response: AdminMeResponse = { authenticated: true, role: 'ADMIN' }
    mockedGet.mockResolvedValue({ data: response } as AxiosResponse<AdminMeResponse>)

    await expect(getAdminMe()).resolves.toEqual(response)

    expect(mockedGet).toHaveBeenCalledWith('/admin/me')
  })
})
