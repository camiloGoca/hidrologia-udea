import { adminHttpClient } from './adminHttpClient'

import type { AdminInterestingLink, UpsertAdminInterestingLinkRequest } from '@/types/interestingLink'

export async function getAdminLinks(): Promise<AdminInterestingLink[]> {
  const response = await adminHttpClient.get<AdminInterestingLink[]>('/admin/links')

  return response.data
}

export async function createAdminLink(payload: UpsertAdminInterestingLinkRequest): Promise<AdminInterestingLink> {
  const response = await adminHttpClient.post<AdminInterestingLink>('/admin/links', payload)

  return response.data
}

export async function updateAdminLink(
  linkId: number,
  payload: UpsertAdminInterestingLinkRequest,
): Promise<AdminInterestingLink> {
  const response = await adminHttpClient.patch<AdminInterestingLink>(`/admin/links/${linkId}`, payload)

  return response.data
}

export async function deleteAdminLink(linkId: number): Promise<void> {
  await adminHttpClient.delete(`/admin/links/${linkId}`)
}
