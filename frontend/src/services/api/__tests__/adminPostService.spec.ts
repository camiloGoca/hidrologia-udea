import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { adminHttpClient } from '@/services/api/adminHttpClient'
import {
  archiveAdminPost,
  createAdminPost,
  discardManualAdminPost,
  getAdminPost,
  getPostsByStatus,
  publishAdminPost,
  restoreAdminPost,
  updateAdminPost,
} from '@/services/api/adminPostService'
import type { AdminPost, AdminPostsResponse } from '@/types/adminPost'
import type { PostContentDocument } from '@/types/postContent'

vi.mock('@/services/api/adminHttpClient', () => ({
  adminHttpClient: {
    get: vi.fn<(url: string, config?: unknown) => Promise<AxiosResponse<unknown>>>(),
    post: vi.fn<(url: string, payload?: unknown) => Promise<AxiosResponse<unknown>>>(),
    patch: vi.fn<(url: string, payload: unknown) => Promise<AxiosResponse<unknown>>>(),
    delete: vi.fn<(url: string) => Promise<AxiosResponse<unknown>>>(),
  },
}))

const mockedGet = vi.mocked(adminHttpClient.get)
const mockedPost = vi.mocked(adminHttpClient.post)
const mockedPatch = vi.mocked(adminHttpClient.patch)
const mockedDelete = vi.mocked(adminHttpClient.delete)

describe('adminPostService', () => {
  beforeEach(() => {
    mockedGet.mockReset()
    mockedPost.mockReset()
    mockedPatch.mockReset()
    mockedDelete.mockReset()
  })

  it('loads posts by status through the admin http client', async () => {
    const response: AdminPostsResponse = {
      items: [],
      page: 1,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    }
    mockedGet.mockResolvedValue({ data: response } as AxiosResponse<AdminPostsResponse>)

    await expect(getPostsByStatus('ARCHIVED', 1, 20)).resolves.toEqual(response)

    expect(mockedGet).toHaveBeenCalledWith('/admin/posts', {
      params: { status: 'ARCHIVED', page: 1, size: 20 },
    })
  })

  it('loads an admin post through the admin http client', async () => {
    const draft = adminPost()
    mockedGet.mockResolvedValue({ data: draft } as AxiosResponse<AdminPost>)

    await expect(getAdminPost(9)).resolves.toEqual(draft)

    expect(mockedGet).toHaveBeenCalledWith('/admin/posts/9')
  })

  it('updates an admin post through the admin http client', async () => {
    const draft = adminPost({
      title: 'Título',
      content: 'Contenido',
    })
    const payload = {
      title: 'Título',
      contentDocument: contentDocument('Contenido'),
      sectionSlug: 'taller-1',
      tagIds: [1, 2],
    }
    mockedPatch.mockResolvedValue({ data: draft } as AxiosResponse<AdminPost>)

    await expect(updateAdminPost(9, payload)).resolves.toEqual(draft)

    expect(mockedPatch).toHaveBeenCalledWith('/admin/posts/9', payload)
  })

  it('sends link content documents unchanged when updating an admin post', async () => {
    const contentDocumentWithLink: PostContentDocument = {
      type: 'doc',
      content: [
        {
          type: 'paragraph',
          content: [
            {
              type: 'text',
              text: 'UdeA',
              marks: [
                {
                  type: 'link',
                  attrs: {
                    href: 'https://www.udea.edu.co',
                    target: '_blank',
                    rel: 'noopener noreferrer',
                    class: null,
                    title: null,
                  },
                },
              ],
            },
          ],
        },
      ],
    }
    const payload = {
      title: 'Título',
      contentDocument: contentDocumentWithLink,
      sectionSlug: 'taller-1',
      tagIds: [],
    }
    const draft = adminPost({ title: 'Título', contentDocument: contentDocumentWithLink })
    mockedPatch.mockResolvedValue({ data: draft } as AxiosResponse<AdminPost>)

    await expect(updateAdminPost(9, payload)).resolves.toEqual(draft)

    expect(mockedPatch).toHaveBeenCalledWith('/admin/posts/9', payload)
  })

  it('creates a manual draft through the admin http client', async () => {
    const draft = adminPost({
      id: 10,
      sourceQuestionId: null,
      sourceQuestion: null,
    })
    const payload = { sectionSlug: 'taller-1' }
    mockedPost.mockResolvedValue({ data: draft } as AxiosResponse<AdminPost>)

    await expect(createAdminPost(payload)).resolves.toEqual(draft)

    expect(mockedPost).toHaveBeenCalledWith('/admin/posts', payload)
  })

  it('publishes archives and restores admin posts through explicit actions', async () => {
    const published = adminPost({
      status: 'PUBLISHED',
      publishedAt: '2026-01-02T00:00:00Z',
    })
    mockedPost.mockResolvedValue({ data: published } as AxiosResponse<AdminPost>)

    await expect(publishAdminPost(9)).resolves.toEqual(published)
    await expect(archiveAdminPost(9)).resolves.toEqual(published)
    await expect(restoreAdminPost(9)).resolves.toEqual(published)

    expect(mockedPost).toHaveBeenNthCalledWith(1, '/admin/posts/9/publish')
    expect(mockedPost).toHaveBeenNthCalledWith(2, '/admin/posts/9/archive')
    expect(mockedPost).toHaveBeenNthCalledWith(3, '/admin/posts/9/restore')
  })

  it('discards a manual draft through the admin http client', async () => {
    mockedDelete.mockResolvedValue({ data: undefined } as AxiosResponse<void>)

    await expect(discardManualAdminPost(10)).resolves.toBeUndefined()

    expect(mockedDelete).toHaveBeenCalledWith('/admin/posts/10')
  })
})

function adminPost(overrides: Partial<AdminPost> = {}): AdminPost {
  const content = overrides.content ?? ''

  return {
    id: 9,
    title: '',
    content,
    contentDocument: overrides.contentDocument ?? contentDocument(content),
    status: 'DRAFT',
    sourceQuestionId: 1,
    section: {
      id: 1,
      type: 'TALLER',
      name: 'Taller 1',
      slug: 'taller-1',
      description: null,
    },
    sourceQuestion: {
      id: 1,
      nickname: null,
      question: 'Pregunta',
      status: 'PENDING',
      createdAt: '2026-01-01T00:00:00Z',
      hasAttachment: false,
    },
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    publishedAt: null,
    tags: [],
    ...overrides,
  }
}

function contentDocument(text: string): PostContentDocument {
  return {
    type: 'doc',
    content: text
      ? [
          {
            type: 'paragraph',
            content: [{ type: 'text', text }],
          },
        ]
      : [{ type: 'paragraph' }],
  }
}
