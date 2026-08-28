import { adminHttpClient } from './adminHttpClient'

import type { AdminAnalyticsSummary } from '@/types/analytics'

export async function getAdminAnalyticsSummary(): Promise<AdminAnalyticsSummary> {
  const response = await adminHttpClient.get<AdminAnalyticsSummary>('/admin/analytics/summary')

  return response.data
}
