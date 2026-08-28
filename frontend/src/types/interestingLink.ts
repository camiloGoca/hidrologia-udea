export interface InterestingLink {
  id: number
  title: string
  description: string | null
  url: string
  displayOrder: number
}

export interface AdminInterestingLink extends InterestingLink {
  active: boolean
}

export interface UpsertAdminInterestingLinkRequest {
  title: string
  description: string | null
  url: string
  displayOrder: number
  active: boolean
}
