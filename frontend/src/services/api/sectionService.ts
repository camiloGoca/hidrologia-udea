import { httpClient } from './httpClient'

import type { Section } from '@/types/section'

export async function getSections(): Promise<Section[]> {
  const response = await httpClient.get<Section[]>('/sections')

  return response.data
}
