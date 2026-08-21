import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { adminHttpClient } from '@/services/api/adminHttpClient'
import {
  archiveQuestion,
  getAdminMe,
  getPendingQuestions,
  getQuestionById,
  getQuestionsByStatus,
  rejectQuestion,
  reopenQuestion,
} from '@/services/api/adminService'
import type { AdminMeResponse } from '@/types/admin'
import type {
  AdminQuestionDetail,
  AdminQuestionStatusUpdateResponse,
  AdminQuestionsResponse,
} from '@/types/adminQuestion'

vi.mock('@/services/api/adminHttpClient', () => ({
  adminHttpClient: {
    get: vi.fn<(url: string, config?: unknown) => Promise<AxiosResponse<unknown>>>(),
    post: vi.fn<(url: string) => Promise<AxiosResponse<unknown>>>(),
  },
}))

const mockedGet = vi.mocked(adminHttpClient.get)
const mockedPost = vi.mocked(adminHttpClient.post)

describe('adminService', () => {
  beforeEach(() => {
    mockedGet.mockReset()
    mockedPost.mockReset()
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
})
