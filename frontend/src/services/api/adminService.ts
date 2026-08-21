import { adminHttpClient } from './adminHttpClient'

import type { AdminMeResponse } from '@/types/admin'

export async function getAdminMe(): Promise<AdminMeResponse> {
  const response = await adminHttpClient.get<AdminMeResponse>('/admin/me')

  return response.data
}
