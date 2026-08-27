import type { AdminQuestionSection, AdminQuestionStatus } from '@/types/adminQuestion'
import type { PostContentDocument } from '@/types/postContent'

export type AdminPostStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface AdminPostSourceQuestion {
  id: number
  nickname: string | null
  question: string
  status: AdminQuestionStatus
  createdAt: string
  hasAttachment: boolean
}

export interface AdminPostTag {
  id: number
  name: string
  slug: string
}

export interface AdminPostImage {
  id: number
  secureUrl: string
  format: string
  width: number
  height: number
  bytes: number
  altText: string
  createdAt: string
}

export interface AdminPost {
  id: number
  title: string
  content: string
  contentDocument: PostContentDocument
  status: AdminPostStatus
  sourceQuestionId: number | null
  section: AdminQuestionSection
  sourceQuestion: AdminPostSourceQuestion | null
  tags: AdminPostTag[]
  images: AdminPostImage[]
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
  contentDocument: PostContentDocument
  sectionSlug: string
  tagIds?: number[] | null
}

export interface CreateAdminPostRequest {
  sectionSlug: string
}
