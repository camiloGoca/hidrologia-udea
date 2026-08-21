import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { adminHttpClient } from '@/services/api/adminHttpClient'
import { httpClient } from '@/services/api/httpClient'
import { getIdToken } from '@/services/firebase/authService'

vi.mock('@/services/firebase/authService', () => ({
  getIdToken: vi.fn<() => Promise<string | null>>(),
}))

const mockedGetIdToken = vi.mocked(getIdToken)

function adapterResponse(config: InternalAxiosRequestConfig): Promise<AxiosResponse> {
  return Promise.resolve({
    data: {},
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  })
}

describe('adminHttpClient', () => {
  beforeEach(() => {
    mockedGetIdToken.mockReset()
    adminHttpClient.defaults.adapter = adapterResponse
    httpClient.defaults.adapter = adapterResponse
  })

  it('adds the Firebase ID token to admin requests', async () => {
    mockedGetIdToken.mockResolvedValue('admin-id-token')

    const response = await adminHttpClient.get('/admin/me')

    expect(response.config.headers.get('Authorization')).toBe('Bearer admin-id-token')
  })

  it('does not add a bearer token to public httpClient requests', async () => {
    mockedGetIdToken.mockResolvedValue('admin-id-token')

    const response = await httpClient.get('/sections')

    expect(response.config.headers.get('Authorization')).toBeUndefined()
    expect(mockedGetIdToken).not.toHaveBeenCalled()
  })
})
