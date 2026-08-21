import { adminHttpClient } from './adminHttpClient'

import type { AdminMeResponse } from '@/types/admin'
import type {
  AdminPendingQuestionsResponse,
  AdminQuestionDetail,
} from '@/types/adminQuestion'

export async function getAdminMe(): Promise<AdminMeResponse> {
  const response = await adminHttpClient.get<AdminMeResponse>('/admin/me')

  return response.data
}

export async function getPendingQuestions(
  page = 0,
  size = 20,
): Promise<AdminPendingQuestionsResponse> {
  const response = await adminHttpClient.get<AdminPendingQuestionsResponse>(
    '/admin/questions/pending',
    {
      params: { page, size },
    },
  )

  return response.data
}

export async function getQuestionById(id: number): Promise<AdminQuestionDetail> {
  const response = await adminHttpClient.get<AdminQuestionDetail>(`/admin/questions/${id}`)

  return response.data
}
