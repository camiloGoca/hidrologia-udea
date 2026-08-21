import { adminHttpClient } from './adminHttpClient'

import type { AdminMeResponse } from '@/types/admin'
import type {
  AdminQuestionStatus,
  AdminQuestionStatusUpdateResponse,
  AdminQuestionsResponse,
  AdminQuestionDetail,
} from '@/types/adminQuestion'

export async function getAdminMe(): Promise<AdminMeResponse> {
  const response = await adminHttpClient.get<AdminMeResponse>('/admin/me')

  return response.data
}

export async function getQuestionsByStatus(
  status: Exclude<AdminQuestionStatus, 'PUBLISHED'>,
  page = 0,
  size = 20,
): Promise<AdminQuestionsResponse> {
  const response = await adminHttpClient.get<AdminQuestionsResponse>('/admin/questions', {
    params: { status, page, size },
  })

  return response.data
}

export async function getPendingQuestions(page = 0, size = 20): Promise<AdminQuestionsResponse> {
  return getQuestionsByStatus('PENDING', page, size)
}

export async function getQuestionById(id: number): Promise<AdminQuestionDetail> {
  const response = await adminHttpClient.get<AdminQuestionDetail>(`/admin/questions/${id}`)

  return response.data
}

export async function rejectQuestion(id: number): Promise<AdminQuestionStatusUpdateResponse> {
  const response = await adminHttpClient.post<AdminQuestionStatusUpdateResponse>(
    `/admin/questions/${id}/reject`,
  )

  return response.data
}

export async function archiveQuestion(id: number): Promise<AdminQuestionStatusUpdateResponse> {
  const response = await adminHttpClient.post<AdminQuestionStatusUpdateResponse>(
    `/admin/questions/${id}/archive`,
  )

  return response.data
}

export async function reopenQuestion(id: number): Promise<AdminQuestionStatusUpdateResponse> {
  const response = await adminHttpClient.post<AdminQuestionStatusUpdateResponse>(
    `/admin/questions/${id}/reopen`,
  )

  return response.data
}
