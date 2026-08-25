import { adminHttpClient } from './adminHttpClient'

import type {
  AdminPost,
  AdminPostsResponse,
  AdminPostStatus,
  CreateAdminPostRequest,
  UpdateAdminPostRequest,
} from '@/types/adminPost'

export async function getPostsByStatus(
  status: AdminPostStatus,
  page = 0,
  size = 20,
): Promise<AdminPostsResponse> {
  const response = await adminHttpClient.get<AdminPostsResponse>('/admin/posts', {
    params: { status, page, size },
  })

  return response.data
}

export async function getAdminPost(postId: number): Promise<AdminPost> {
  const response = await adminHttpClient.get<AdminPost>(`/admin/posts/${postId}`)

  return response.data
}

export async function createAdminPost(payload: CreateAdminPostRequest): Promise<AdminPost> {
  const response = await adminHttpClient.post<AdminPost>('/admin/posts', payload)

  return response.data
}

export async function updateAdminPost(
  postId: number,
  payload: UpdateAdminPostRequest,
): Promise<AdminPost> {
  const response = await adminHttpClient.patch<AdminPost>(`/admin/posts/${postId}`, payload)

  return response.data
}

export async function publishAdminPost(postId: number): Promise<AdminPost> {
  const response = await adminHttpClient.post<AdminPost>(`/admin/posts/${postId}/publish`)

  return response.data
}

export async function archiveAdminPost(postId: number): Promise<AdminPost> {
  const response = await adminHttpClient.post<AdminPost>(`/admin/posts/${postId}/archive`)

  return response.data
}

export async function restoreAdminPost(postId: number): Promise<AdminPost> {
  const response = await adminHttpClient.post<AdminPost>(`/admin/posts/${postId}/restore`)

  return response.data
}

export async function discardManualAdminPost(postId: number): Promise<void> {
  await adminHttpClient.delete(`/admin/posts/${postId}`)
}
