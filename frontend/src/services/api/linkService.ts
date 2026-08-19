import { httpClient } from './httpClient'

import type { InterestingLink } from '@/types/interestingLink'

export async function getInterestingLinks(): Promise<InterestingLink[]> {
  const response = await httpClient.get<InterestingLink[]>('/links')

  return response.data
}
