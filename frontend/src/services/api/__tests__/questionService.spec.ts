import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '@/services/api/httpClient'
import { createQuestion } from '@/services/api/questionService'
import type {
  CreateStudentQuestionRequest,
  CreateStudentQuestionResponse,
} from '@/types/studentQuestion'

vi.mock('@/services/api/httpClient', () => ({
  httpClient: {
    post: vi.fn<
      (url: string, payload: CreateStudentQuestionRequest) => Promise<AxiosResponse<CreateStudentQuestionResponse>>
    >(),
  },
}))

const mockedPost = vi.mocked(httpClient.post)

describe('questionService', () => {
  beforeEach(() => {
    mockedPost.mockReset()
  })

  it('posts a student question and returns the creation response', async () => {
    const payload: CreateStudentQuestionRequest = {
      sectionSlug: 'taller-1',
      nickname: 'Estudiante',
      question: 'Pregunta de prueba',
    }
    const response: CreateStudentQuestionResponse = {
      id: 1,
      status: 'PENDING',
      createdAt: '2026-01-01T00:00:00Z',
    }

    mockedPost.mockResolvedValue({ data: response } as AxiosResponse<CreateStudentQuestionResponse>)

    await expect(createQuestion(payload)).resolves.toEqual(response)
    expect(mockedPost).toHaveBeenCalledWith('/questions', payload)
  })
})
