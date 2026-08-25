export interface AdminTag {
  id: number
  name: string
  slug: string
  usageCount: number
}

export interface UpsertAdminTagRequest {
  name: string
}
