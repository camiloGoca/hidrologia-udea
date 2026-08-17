export type SectionType = 'TALLER' | 'PARCIAL'

export interface Section {
  id: number
  type: SectionType
  name: string
  slug: string
  description: string | null
  displayOrder: number
}
