import { adminHttpClient } from './adminHttpClient'

import type { AdminTag, UpsertAdminTagRequest } from '@/types/adminTag'

export async function getAdminTags(): Promise<AdminTag[]> {
  const response = await adminHttpClient.get<AdminTag[]>('/admin/tags')

  return response.data
}

export async function createAdminTag(payload: UpsertAdminTagRequest): Promise<AdminTag> {
  const response = await adminHttpClient.post<AdminTag>('/admin/tags', payload)

  return response.data
}

export async function renameAdminTag(tagId: number, payload: UpsertAdminTagRequest): Promise<AdminTag> {
  const response = await adminHttpClient.patch<AdminTag>(`/admin/tags/${tagId}`, payload)

  return response.data
}

export async function deleteAdminTag(tagId: number): Promise<void> {
  await adminHttpClient.delete(`/admin/tags/${tagId}`)
}
