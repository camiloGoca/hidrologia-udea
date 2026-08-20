export const QUESTION_MAX_LENGTH = 2000
export const NICKNAME_MAX_LENGTH = 80

export type StudentQuestionStatus = 'PENDING' | 'PUBLISHED' | 'ARCHIVED' | 'REJECTED'

export interface CreateStudentQuestionRequest {
  sectionSlug: string
  nickname: string | null
  question: string
}

export interface CreateStudentQuestionResponse {
  id: number
  status: StudentQuestionStatus
  createdAt: string
}
