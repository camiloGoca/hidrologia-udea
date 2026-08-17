import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getSections } from '@/services/api/sectionService'
import type { Section } from '@/types/section'
import ParcialesView from '@/views/sections/ParcialesView.vue'
import TalleresView from '@/views/sections/TalleresView.vue'

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

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>',
}

describe('section views', () => {
  beforeEach(() => {
    mockedGetSections.mockReset()
  })

  it('shows loading while sections are requested', () => {
    mockedGetSections.mockReturnValue(new Promise(() => undefined))

    const wrapper = mount(TalleresView, {
      global: {
        stubs: { RouterLink: routerLinkStub },
      },
    })

    expect(wrapper.text()).toContain('Cargando secciones...')
  })

  it('shows a friendly error message when sections fail', async () => {
    mockedGetSections.mockRejectedValue(new Error('Network error'))

    const wrapper = mount(TalleresView, {
      global: {
        stubs: { RouterLink: routerLinkStub },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar los talleres')
  })

  it('renders only talleres on TalleresView', async () => {
    mockedGetSections.mockResolvedValue(sections)

    const wrapper = mount(TalleresView, {
      global: {
        stubs: { RouterLink: routerLinkStub },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Taller 1')
    expect(wrapper.text()).toContain('Morfometría de cuencas')
    expect(wrapper.text()).toContain('Taller 2')
    expect(wrapper.text()).not.toContain('Parcial 1')
  })

  it('renders only parciales on ParcialesView', async () => {
    mockedGetSections.mockResolvedValue(sections)

    const wrapper = mount(ParcialesView, {
      global: {
        stubs: { RouterLink: routerLinkStub },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Parcial 1')
    expect(wrapper.text()).not.toContain('Taller 1')
    expect(wrapper.text()).not.toContain('Taller 2')
  })

  it('shows an empty state when there are no matching sections', async () => {
    mockedGetSections.mockResolvedValue([])

    const wrapper = mount(ParcialesView, {
      global: {
        stubs: { RouterLink: routerLinkStub },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Todavía no hay parciales disponibles')
  })
})
