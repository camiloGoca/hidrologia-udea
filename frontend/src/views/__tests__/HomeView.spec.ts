import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getSections } from '@/services/api/sectionService'
import type { Section } from '@/types/section'
import HomeView from '@/views/HomeView.vue'

vi.mock('@/services/api/sectionService', () => ({
  getSections: vi.fn<() => Promise<Section[]>>(),
}))

const mockedGetSections = vi.mocked(getSections)

const sections: Section[] = [
  {
    id: 1,
    type: 'TALLER',
    name: 'Taller 1',
    slug: 'taller-1',
    description: 'Morfometría de cuencas',
    displayOrder: 1,
  },
  {
    id: 2,
    type: 'TALLER',
    name: 'Taller 2',
    slug: 'taller-2',
    description: 'Estadística y balance hídrico',
    displayOrder: 2,
  },
  {
    id: 4,
    type: 'PARCIAL',
    name: 'Parcial 1',
    slug: 'parcial-1',
    description: null,
    displayOrder: 4,
  },
]

describe('HomeView', () => {
  beforeEach(() => {
    mockedGetSections.mockReset()
  })

  it('shows loading while sections are being requested', () => {
    mockedGetSections.mockReturnValue(new Promise(() => undefined))

    const wrapper = mount(HomeView)

    expect(wrapper.text()).toContain('Cargando secciones...')
  })

  it('renders talleres and parciales from the API response', async () => {
    mockedGetSections.mockResolvedValue(sections)

    const wrapper = mount(HomeView)
    await flushPromises()

    expect(wrapper.text()).toContain('Talleres')
    expect(wrapper.text()).toContain('Taller 1')
    expect(wrapper.text()).toContain('Morfometría de cuencas')
    expect(wrapper.text()).toContain('Taller 2')
    expect(wrapper.text()).toContain('Estadística y balance hídrico')
    expect(wrapper.text()).toContain('Parciales')
    expect(wrapper.text()).toContain('Parcial 1')
  })

  it('shows a friendly error message when the API fails', async () => {
    mockedGetSections.mockRejectedValue(new Error('Network error'))

    const wrapper = mount(HomeView)
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar las secciones')
  })

  it('shows an empty state when the API returns no sections', async () => {
    mockedGetSections.mockResolvedValue([])

    const wrapper = mount(HomeView)
    await flushPromises()

    expect(wrapper.text()).toContain('Todavía no hay secciones disponibles')
  })
})
