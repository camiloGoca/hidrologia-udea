export const QUESTION_MAX_LENGTH = 2000
export const NICKNAME_MAX_LENGTH = 80
export const QUESTION_IMAGE_MAX_SIZE_BYTES = 5 * 1024 * 1024
export const QUESTION_IMAGE_ACCEPTED_TYPES = ['image/jpeg', 'image/png'] as const

export type StudentQuestionStatus = 'PENDING' | 'PUBLISHED' | 'ARCHIVED' | 'REJECTED'
export type QuestionImageAcceptedType = (typeof QUESTION_IMAGE_ACCEPTED_TYPES)[number]

export interface CreateStudentQuestionRequest {
  sectionSlug: string
  nickname: string | null
  question: string
}

export interface CreateStudentQuestionPayload {
  data: CreateStudentQuestionRequest
  image?: File | null
}

export interface CreateStudentQuestionResponse {
  id: number
  status: StudentQuestionStatus
  createdAt: string
}
