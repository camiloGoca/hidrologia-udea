import type { AxiosResponse } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '@/services/api/httpClient'
import {
  getOrCreateAnalyticsSessionId,
  getPublicVisitCount,
  recordPostView,
  recordSectionView,
  recordSiteVisit,
} from '@/services/api/analyticsService'
import type { PublicVisitCount } from '@/types/analytics'

vi.mock('@/services/api/httpClient', () => ({
  httpClient: {
    get: vi.fn<(url: string) => Promise<AxiosResponse<PublicVisitCount>>>(),
    post: vi.fn<(url: string, payload: unknown) => Promise<AxiosResponse<void>>>(),
  },
}))

const mockedGet = vi.mocked(httpClient.get)
const mockedPost = vi.mocked(httpClient.post)
const sessionId = '11111111-1111-4111-8111-111111111111'

describe('analyticsService', () => {
  beforeEach(() => {
    sessionStorage.clear()
    mockedGet.mockReset()
    mockedPost.mockReset()
    mockedPost.mockResolvedValue({ data: undefined } as AxiosResponse<void>)
    vi.stubGlobal('crypto', {
      randomUUID: vi.fn<() => string>(() => sessionId),
      getRandomValues: vi.fn<(values: Uint8Array) => Uint8Array>((values) => values.fill(1)),
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('creates and reuses an anonymous browser-session id', () => {
    expect(getOrCreateAnalyticsSessionId()).toBe(sessionId)
    expect(getOrCreateAnalyticsSessionId()).toBe(sessionId)
  })

  it('records site, section and post views with the session id only', async () => {
    await recordSiteVisit()
    await recordSectionView('taller-1')
    await recordPostView(10)

    expect(mockedPost).toHaveBeenNthCalledWith(1, '/analytics/visit', { sessionId })
    expect(mockedPost).toHaveBeenNthCalledWith(2, '/analytics/sections/taller-1/view', { sessionId })
    expect(mockedPost).toHaveBeenNthCalledWith(3, '/analytics/posts/10/view', { sessionId })
  })

  it('loads the public visit count', async () => {
    mockedGet.mockResolvedValue({ data: { visits: 12 } } as AxiosResponse<PublicVisitCount>)

    await expect(getPublicVisitCount()).resolves.toEqual({ visits: 12 })

    expect(mockedGet).toHaveBeenCalledWith('/analytics/visits/count')
  })
})
