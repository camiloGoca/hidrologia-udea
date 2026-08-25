import type { AdminQuestionSection, AdminQuestionStatus } from '@/types/adminQuestion'

export type AdminPostStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface AdminPostSourceQuestion {
  id: number
  nickname: string | null
  question: string
  status: AdminQuestionStatus
  createdAt: string
  hasAttachment: boolean
}

export interface AdminPost {
  id: number
  title: string
  content: string
  status: AdminPostStatus
  sourceQuestionId: number | null
  section: AdminQuestionSection
  sourceQuestion: AdminPostSourceQuestion | null
  createdAt: string
  updatedAt: string
  publishedAt: string | null
}

export interface AdminPostSummary {
  id: number
  title: string
  status: AdminPostStatus
  section: AdminQuestionSection
  hasSourceQuestion: boolean
  sourceQuestionId: number | null
  createdAt: string
  updatedAt: string
  publishedAt: string | null
}

export interface AdminPostsResponse {
  items: AdminPostSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface UpdateAdminPostRequest {
  title: string
  content: string
  sectionSlug: string
}
