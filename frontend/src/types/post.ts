import type { SectionType } from './section'
import type { PostContentDocument } from './postContent'

export interface PostTag {
  name: string
  slug: string
}

export interface PostSection {
  id: number
  type: SectionType
  name: string
  slug: string
  description: string | null
}

export interface PostSummary {
  id: number
  title: string
  section: PostSection
  tags: PostTag[]
  publishedAt: string
}

export interface PostSearchResult extends PostSummary {
  snippet: string
}

export interface PostImage {
  id: number
  secureUrl: string
  width: number
  height: number
  altText: string
}

export interface PostDetail extends PostSummary {
  content: string
  contentDocument: PostContentDocument
  images: PostImage[]
}

export interface SectionPostsResponse {
  section: PostSection
  posts: PostSummary[]
}

export interface TagPostsResponse {
  tag: PostTag
  posts: PostSummary[]
}
