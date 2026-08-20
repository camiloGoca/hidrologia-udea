import { httpClient } from './httpClient'

import type {
  CreateStudentQuestionRequest,
  CreateStudentQuestionResponse,
} from '@/types/studentQuestion'

export async function createQuestion(
  payload: CreateStudentQuestionRequest,
): Promise<CreateStudentQuestionResponse> {
  const response = await httpClient.post<CreateStudentQuestionResponse>('/questions', payload)

  return response.data
}
