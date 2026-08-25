import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { adminHttpClient } from '@/services/api/adminHttpClient'
import {
  archiveQuestion,
  createQuestionDraft,
  discardQuestionDraft,
  getAdminMe,
  getAdminPost,
  getPendingQuestions,
  getQuestionById,
  getQuestionsByStatus,
  publishAdminPost,
  rejectQuestion,
  reopenQuestion,
  updateAdminPostDraft,
} from '@/services/api/adminService'
import type { AdminMeResponse } from '@/types/admin'
import type { AdminPost } from '@/types/adminPost'
import type {
  AdminQuestionDetail,
  AdminQuestionStatusUpdateResponse,
  AdminQuestionsResponse,
} from '@/types/adminQuestion'

vi.mock('@/services/api/adminHttpClient', () => ({
  adminHttpClient: {
    get: vi.fn<(url: string, config?: unknown) => Promise<AxiosResponse<unknown>>>(),
    post: vi.fn<(url: string) => Promise<AxiosResponse<unknown>>>(),
    patch: vi.fn<(url: string, payload: unknown) => Promise<AxiosResponse<unknown>>>(),
    delete: vi.fn<(url: string) => Promise<AxiosResponse<unknown>>>(),
  },
}))

const mockedGet = vi.mocked(adminHttpClient.get)
const mockedPost = vi.mocked(adminHttpClient.post)
const mockedPatch = vi.mocked(adminHttpClient.patch)
const mockedDelete = vi.mocked(adminHttpClient.delete)

describe('adminService', () => {
  beforeEach(() => {
    mockedGet.mockReset()
    mockedPost.mockReset()
    mockedPatch.mockReset()
    mockedDelete.mockReset()
  })

  it('loads the authorized administrator session', async () => {
    const response: AdminMeResponse = { authenticated: true, role: 'ADMIN' }
    mockedGet.mockResolvedValue({ data: response } as AxiosResponse<AdminMeResponse>)

    await expect(getAdminMe()).resolves.toEqual(response)

    expect(mockedGet).toHaveBeenCalledWith('/admin/me')
  })

  it('loads questions by status through the admin http client', async () => {
    const response: AdminQuestionsResponse = {
      items: [],
      page: 1,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    }
    mockedGet.mockResolvedValue({ data: response } as AxiosResponse<AdminQuestionsResponse>)

    await expect(getQuestionsByStatus('ARCHIVED', 1, 20)).resolves.toEqual(response)

    expect(mockedGet).toHaveBeenCalledWith('/admin/questions', {
      params: { status: 'ARCHIVED', page: 1, size: 20 },
    })
  })

  it('keeps pending questions helper as a status-specific call', async () => {
    const response: AdminQuestionsResponse = {
      items: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    }
    mockedGet.mockResolvedValue({ data: response } as AxiosResponse<AdminQuestionsResponse>)

    await expect(getPendingQuestions(0, 20)).resolves.toEqual(response)

    expect(mockedGet).toHaveBeenCalledWith('/admin/questions', {
      params: { status: 'PENDING', page: 0, size: 20 },
    })
  })

  it('loads a question detail through the admin http client', async () => {
    const response: AdminQuestionDetail = {
      id: 1,
      nickname: null,
      question: 'Pregunta',
      status: 'PENDING',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
      section: {
        id: 1,
        type: 'TALLER',
        name: 'Taller 1',
        slug: 'taller-1',
        description: null,
      },
      attachment: null,
      linkedPost: null,
    }
    mockedGet.mockResolvedValue({ data: response } as AxiosResponse<AdminQuestionDetail>)

    await expect(getQuestionById(1)).resolves.toEqual(response)

    expect(mockedGet).toHaveBeenCalledWith('/admin/questions/1')
  })

  it('sends explicit status actions through the admin http client', async () => {
    const response: AdminQuestionStatusUpdateResponse = {
      id: 1,
      status: 'REJECTED',
      updatedAt: '2026-01-01T00:00:30Z',
    }
    mockedPost.mockResolvedValue({ data: response } as AxiosResponse<AdminQuestionStatusUpdateResponse>)

    await expect(rejectQuestion(1)).resolves.toEqual(response)
    await expect(archiveQuestion(2)).resolves.toEqual(response)
    await expect(reopenQuestion(3)).resolves.toEqual(response)

    expect(mockedPost).toHaveBeenNthCalledWith(1, '/admin/questions/1/reject')
    expect(mockedPost).toHaveBeenNthCalledWith(2, '/admin/questions/2/archive')
    expect(mockedPost).toHaveBeenNthCalledWith(3, '/admin/questions/3/reopen')
  })

  it('creates and discards a question draft through the admin http client', async () => {
    const draft = adminPost()
    mockedPost.mockResolvedValue({ data: draft } as AxiosResponse<AdminPost>)
    mockedDelete.mockResolvedValue({ data: undefined } as AxiosResponse<void>)

    await expect(createQuestionDraft(1)).resolves.toEqual(draft)
    await expect(discardQuestionDraft(1)).resolves.toBeUndefined()

    expect(mockedPost).toHaveBeenCalledWith('/admin/questions/1/draft')
    expect(mockedDelete).toHaveBeenCalledWith('/admin/questions/1/draft')
  })

  it('loads an admin post through the admin http client', async () => {
    const draft = adminPost()
    mockedGet.mockResolvedValue({ data: draft } as AxiosResponse<AdminPost>)

    await expect(getAdminPost(9)).resolves.toEqual(draft)

    expect(mockedGet).toHaveBeenCalledWith('/admin/posts/9')
  })

  it('updates an admin post draft through the admin http client', async () => {
    const draft = adminPost({
      title: 'Título',
      content: 'Contenido',
    })
    const payload = {
      title: 'Título',
      content: 'Contenido',
      sectionSlug: 'taller-1',
    }
    mockedPatch.mockResolvedValue({ data: draft } as AxiosResponse<AdminPost>)

    await expect(updateAdminPostDraft(9, payload)).resolves.toEqual(draft)

    expect(mockedPatch).toHaveBeenCalledWith('/admin/posts/9', payload)
  })

  it('publishes an admin post through the admin http client', async () => {
    const published = adminPost({
      status: 'PUBLISHED',
      publishedAt: '2026-01-02T00:00:00Z',
    })
    mockedPost.mockResolvedValue({ data: published } as AxiosResponse<AdminPost>)

    await expect(publishAdminPost(9)).resolves.toEqual(published)

    expect(mockedPost).toHaveBeenCalledWith('/admin/posts/9/publish')
  })
})

function adminPost(overrides: Partial<AdminPost> = {}): AdminPost {
  return {
    id: 9,
    title: '',
    content: '',
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
    ...overrides,
  }
}
