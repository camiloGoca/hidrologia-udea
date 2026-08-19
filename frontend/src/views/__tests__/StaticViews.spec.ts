import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getInterestingLinks } from '@/services/api/linkService'
import type { InterestingLink } from '@/types/interestingLink'
import LinksView from '@/views/static/LinksView.vue'
import NewQuestionView from '@/views/static/NewQuestionView.vue'
import NotFoundView from '@/views/static/NotFoundView.vue'

vi.mock('@/services/api/linkService', () => ({
  getInterestingLinks: vi.fn<() => Promise<InterestingLink[]>>(),
}))

const mockedGetInterestingLinks = vi.mocked(getInterestingLinks)

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>',
}

describe('static public views', () => {
  beforeEach(() => {
    mockedGetInterestingLinks.mockReset()
  })

  it('shows loading while links are requested', () => {
    mockedGetInterestingLinks.mockReturnValue(new Promise(() => undefined))

    const wrapper = mount(LinksView, {
      global: {
        stubs: { RouterLink: routerLinkStub },
      },
    })

    expect(wrapper.text()).toContain('Cargando recursos...')
  })

  it('shows an empty state when there are no links', async () => {
    mockedGetInterestingLinks.mockResolvedValue([])

    const wrapper = mount(LinksView, {
      global: {
        stubs: { RouterLink: routerLinkStub },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Aún no hay enlaces de interés publicados.')
  })

  it('renders approved links returned by the API service', async () => {
    mockedGetInterestingLinks.mockResolvedValue([
      {
        id: 1,
        title: 'Recurso aprobado',
        description: 'Descripción pública opcional',
        url: 'https://example.edu/recurso',
        displayOrder: 10,
      },
    ])

    const wrapper = mount(LinksView, {
      global: {
        stubs: { RouterLink: routerLinkStub },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Recurso aprobado')
    expect(wrapper.text()).toContain('Descripción pública opcional')

    const link = wrapper.get('a[href="https://example.edu/recurso"]')

    expect(link.attributes('target')).toBe('_blank')
    expect(link.attributes('rel')).toBe('noopener noreferrer')
  })

  it('shows a friendly error when links fail', async () => {
    mockedGetInterestingLinks.mockRejectedValue(new Error('Network error'))

    const wrapper = mount(LinksView, {
      global: {
        stubs: { RouterLink: routerLinkStub },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar los enlaces de interés.')
  })

  it('renders NewQuestionView as a structural page', () => {
    const wrapper = mount(NewQuestionView)

    expect(wrapper.text()).toContain('Agregar una pregunta')
    expect(wrapper.text()).toContain('enviar una pregunta')
  })

  it('renders NotFoundView', () => {
    const wrapper = mount(NotFoundView, {
      global: {
        stubs: { RouterLink: routerLinkStub },
      },
    })

    expect(wrapper.text()).toContain('no encontrada')
  })
})
