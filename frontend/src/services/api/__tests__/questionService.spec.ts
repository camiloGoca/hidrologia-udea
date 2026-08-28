import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '@/services/api/httpClient'
import { createQuestion } from '@/services/api/questionService'
import type { CreateStudentQuestionResponse } from '@/types/studentQuestion'

vi.mock('@/services/api/httpClient', () => ({
  httpClient: {
    post: vi.fn<(url: string, payload: FormData) => Promise<AxiosResponse<CreateStudentQuestionResponse>>>(),
  },
}))

const mockedPost = vi.mocked(httpClient.post)

describe('questionService', () => {
  beforeEach(() => {
    mockedPost.mockReset()
  })

  it('posts a student question as multipart FormData without image', async () => {
    const response: CreateStudentQuestionResponse = {
      id: 1,
      status: 'PENDING',
      createdAt: '2026-01-01T00:00:00Z',
    }

    mockedPost.mockResolvedValue({ data: response } as AxiosResponse<CreateStudentQuestionResponse>)

    await expect(
      createQuestion({
        data: {
          sectionSlug: 'taller-1',
          nickname: 'Estudiante',
          question: 'Pregunta de prueba',
          turnstileToken: 'valid-turnstile-token',
        },
      }),
    ).resolves.toEqual(response)

    expect(mockedPost).toHaveBeenCalledWith('/questions', expect.any(FormData))
    const formData = postedFormData()

    expect(formData.get('data')).toBeInstanceOf(Blob)
    await expect(postedJson()).resolves.toEqual({
      sectionSlug: 'taller-1',
      nickname: 'Estudiante',
      question: 'Pregunta de prueba',
      turnstileToken: 'valid-turnstile-token',
    })
    expect(formData.has('image')).toBe(false)
  })

  it('includes the optional image when provided', async () => {
    const image = new File(['image'], 'image.png', { type: 'image/png' })
    const response: CreateStudentQuestionResponse = {
      id: 1,
      status: 'PENDING',
      createdAt: '2026-01-01T00:00:00Z',
    }

    mockedPost.mockResolvedValue({ data: response } as AxiosResponse<CreateStudentQuestionResponse>)

    await createQuestion({
      data: {
        sectionSlug: 'taller-1',
        nickname: null,
        question: 'Pregunta de prueba',
        turnstileToken: null,
      },
      image,
    })

    const formData = postedFormData()

    expect(formData.get('image')).toBe(image)
  })

  it('keeps the previous call shape by wrapping the JSON payload in FormData', async () => {
    const response: CreateStudentQuestionResponse = {
      id: 1,
      status: 'PENDING',
      createdAt: '2026-01-01T00:00:00Z',
    }

    mockedPost.mockResolvedValue({ data: response } as AxiosResponse<CreateStudentQuestionResponse>)

    await createQuestion({
      sectionSlug: 'taller-1',
      nickname: null,
      question: 'Pregunta de prueba',
      turnstileToken: null,
    })

    expect(mockedPost).toHaveBeenCalledWith('/questions', expect.any(FormData))
  })
})

function postedFormData() {
  const call = mockedPost.mock.calls[0]
  expect(call).toBeDefined()

  return call![1] as FormData
}

async function postedJson() {
  const data = postedFormData().get('data')
  expect(data).toBeInstanceOf(Blob)

  return JSON.parse(await (data as Blob).text()) as unknown
}
