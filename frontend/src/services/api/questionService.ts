import { httpClient } from './httpClient'

import type {
  CreateStudentQuestionPayload,
  CreateStudentQuestionRequest,
  CreateStudentQuestionResponse,
} from '@/types/studentQuestion'

export async function createQuestion(
  payload: CreateStudentQuestionRequest | CreateStudentQuestionPayload,
): Promise<CreateStudentQuestionResponse> {
  const multipartPayload = 'data' in payload ? payload : { data: payload, image: null }
  const formData = new FormData()

  formData.append(
    'data',
    new Blob([JSON.stringify(multipartPayload.data)], { type: 'application/json' }),
  )

  if (multipartPayload.image) {
    formData.append('image', multipartPayload.image)
  }

  const response = await httpClient.post<CreateStudentQuestionResponse>('/questions', formData)

  return response.data
}
