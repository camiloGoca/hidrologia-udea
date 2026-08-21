import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { adminHttpClient } from '@/services/api/adminHttpClient'
import { getAdminMe, getPendingQuestions, getQuestionById } from '@/services/api/adminService'
import type { AdminMeResponse } from '@/types/admin'
import type { AdminPendingQuestionsResponse, AdminQuestionDetail } from '@/types/adminQuestion'

vi.mock('@/services/api/adminHttpClient', () => ({
  adminHttpClient: {
    get: vi.fn<(url: string, config?: unknown) => Promise<AxiosResponse<unknown>>>(),
  },
}))

const mockedGet = vi.mocked(adminHttpClient.get)

describe('adminService', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  it('loads the authorized administrator session', async () => {
    const response: AdminMeResponse = { authenticated: true, role: 'ADMIN' }
    mockedGet.mockResolvedValue({ data: response } as AxiosResponse<AdminMeResponse>)

    await expect(getAdminMe()).resolves.toEqual(response)

    expect(mockedGet).toHaveBeenCalledWith('/admin/me')
  })

  it('loads pending questions through the admin http client', async () => {
    const response: AdminPendingQuestionsResponse = {
      items: [],
      page: 1,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    }
    mockedGet.mockResolvedValue({ data: response } as AxiosResponse<AdminPendingQuestionsResponse>)

    await expect(getPendingQuestions(1, 20)).resolves.toEqual(response)

    expect(mockedGet).toHaveBeenCalledWith('/admin/questions/pending', {
      params: { page: 1, size: 20 },
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
})
