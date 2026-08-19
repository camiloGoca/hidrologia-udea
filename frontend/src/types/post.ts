import type { SectionType } from './section'

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

export interface PostDetail extends PostSummary {
  content: string
}

export interface SectionPostsResponse {
  section: PostSection
  posts: PostSummary[]
}

export interface TagPostsResponse {
  tag: PostTag
  posts: PostSummary[]
}
