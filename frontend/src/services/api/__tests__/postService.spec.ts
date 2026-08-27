import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '@/services/api/httpClient'
import { getPostById, getPostsBySection, getPostsByTag } from '@/services/api/postService'
import type { PostDetail, SectionPostsResponse, TagPostsResponse } from '@/types/post'

vi.mock('@/services/api/httpClient', () => ({
  httpClient: {
    get: vi.fn<(url: string) => Promise<AxiosResponse<unknown>>>(),
  },
}))

const mockedGet = vi.mocked(httpClient.get)

describe('postService', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  it('gets published posts for a section', async () => {
    const payload: SectionPostsResponse = {
      section: {
        id: 1,
        type: 'TALLER',
        name: 'Taller 1',
        slug: 'taller-1',
        description: 'Morfometría de cuencas',
      },
      posts: [],
    }

    mockedGet.mockResolvedValue({ data: payload } as AxiosResponse<SectionPostsResponse>)

    await expect(getPostsBySection('taller-1')).resolves.toEqual(payload)
    expect(mockedGet).toHaveBeenCalledWith('/sections/taller-1/posts')
  })

  it('gets a published post detail', async () => {
    const payload: PostDetail = {
      id: 10,
      title: 'Pregunta publicada',
      content: 'Contenido de la solución',
      contentDocument: {
        type: 'doc',
        content: [
          {
            type: 'paragraph',
            content: [{ type: 'text', text: 'Contenido de la solución' }],
          },
        ],
      },
      section: {
        id: 1,
        type: 'TALLER',
        name: 'Taller 1',
        slug: 'taller-1',
        description: 'Morfometría de cuencas',
      },
      tags: [{ name: 'Cuencas', slug: 'cuencas' }],
      images: [],
      publishedAt: '2026-01-02T00:00:00Z',
    }

    mockedGet.mockResolvedValue({ data: payload } as AxiosResponse<PostDetail>)

    await expect(getPostById(10)).resolves.toEqual(payload)
    expect(mockedGet).toHaveBeenCalledWith('/posts/10')
  })

  it('gets published posts for a hashtag', async () => {
    const payload: TagPostsResponse = {
      tag: { name: 'Cuencas', slug: 'cuencas' },
      posts: [],
    }

    mockedGet.mockResolvedValue({ data: payload } as AxiosResponse<TagPostsResponse>)

    await expect(getPostsByTag('cuencas')).resolves.toEqual(payload)
    expect(mockedGet).toHaveBeenCalledWith('/tags/cuencas/posts')
  })
})
