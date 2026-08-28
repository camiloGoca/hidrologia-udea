import { httpClient } from './httpClient'

import type { PostDetail, PostSearchResult, SectionPostsResponse, TagPostsResponse } from '@/types/post'

export async function getPostsBySection(slug: string): Promise<SectionPostsResponse> {
  const response = await httpClient.get<SectionPostsResponse>(`/sections/${slug}/posts`)

  return response.data
}

export async function getPostById(id: number | string): Promise<PostDetail> {
  const response = await httpClient.get<PostDetail>(`/posts/${id}`)

  return response.data
}

export async function searchPosts(query: string): Promise<PostSearchResult[]> {
  const response = await httpClient.get<PostSearchResult[]>('/posts/search', {
    params: { q: query },
  })

  return response.data
}

export async function getPostsByTag(slug: string): Promise<TagPostsResponse> {
  const response = await httpClient.get<TagPostsResponse>(`/tags/${slug}/posts`)

  return response.data
}
