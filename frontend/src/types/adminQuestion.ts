export type AdminQuestionStatus = 'PENDING' | 'PUBLISHED' | 'ARCHIVED' | 'REJECTED'

export interface AdminQuestionSection {
  id: number
  type: 'TALLER' | 'PARCIAL'
  name: string
  slug: string
  description: string | null
}

export interface AdminQuestionSummary {
  id: number
  nickname: string | null
  section: AdminQuestionSection
  status: AdminQuestionStatus
  questionPreview: string
  hasAttachment: boolean
  hasLinkedPost: boolean
  createdAt: string
}

export interface AdminQuestionsResponse {
  items: AdminQuestionSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface AdminQuestionAttachment {
  secureUrl: string
  format: string
  width: number
  height: number
  bytes: number
}

export interface AdminQuestionDetail {
  id: number
  nickname: string | null
  question: string
  status: AdminQuestionStatus
  createdAt: string
  updatedAt: string
  section: AdminQuestionSection
  attachment: AdminQuestionAttachment | null
  linkedPost: AdminLinkedPost | null
}

export interface AdminQuestionStatusUpdateResponse {
  id: number
  status: AdminQuestionStatus
  updatedAt: string
}

export interface AdminLinkedPost {
  id: number
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
  title: string
}
