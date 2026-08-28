import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { adminHttpClient } from '@/services/api/adminHttpClient'
import { getAdminAnalyticsSummary } from '@/services/api/adminAnalyticsService'
import type { AdminAnalyticsSummary } from '@/types/analytics'

vi.mock('@/services/api/adminHttpClient', () => ({
  adminHttpClient: {
    get: vi.fn<(url: string) => Promise<AxiosResponse<AdminAnalyticsSummary>>>(),
  },
}))

const mockedGet = vi.mocked(adminHttpClient.get)

describe('adminAnalyticsService', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  it('loads the admin summary through the admin http client', async () => {
    const summary = analyticsSummary()
    mockedGet.mockResolvedValue({ data: summary } as AxiosResponse<AdminAnalyticsSummary>)

    await expect(getAdminAnalyticsSummary()).resolves.toEqual(summary)

    expect(mockedGet).toHaveBeenCalledWith('/admin/analytics/summary')
  })
})

function analyticsSummary(): AdminAnalyticsSummary {
  return {
    totalVisits: 10,
    visitsToday: 2,
    visitsThisWeek: 5,
    visitsThisMonth: 8,
    mostViewedSections: [],
    mostViewedWorkshop: null,
    mostViewedExam: null,
    mostViewedPosts: [],
    questions: {
      total: 3,
      pending: 1,
      published: 2,
    },
    dailyVisits: [],
  }
}
