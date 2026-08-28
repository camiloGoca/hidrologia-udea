import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { recordSectionView } from '@/services/api/analyticsService'
import { getPostsBySection } from '@/services/api/postService'
import type { SectionPostsResponse } from '@/types/post'
import SectionPlaceholderView from '@/views/sections/SectionPlaceholderView.vue'

const routeState = vi.hoisted(() => ({
  params: {
    slug: 'taller-1',
  },
}))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRoute: () => routeState,
  }
})

vi.mock('@/services/api/postService', () => ({
  getPostsBySection: vi.fn<() => Promise<SectionPostsResponse>>(),
}))

vi.mock('@/services/api/analyticsService', () => ({
  recordSectionView: vi.fn<() => Promise<void>>(),
}))

const mockedGetPostsBySection = vi.mocked(getPostsBySection)
const mockedRecordSectionView = vi.mocked(recordSectionView)

const routerLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a><slot /></a>',
}

const emptyResponse: SectionPostsResponse = {
  section: {
    id: 1,
    type: 'TALLER',
    name: 'Taller 1',
    slug: 'taller-1',
    description: 'Morfometría de cuencas',
  },
  posts: [],
}

describe('section posts view', () => {
  beforeEach(() => {
    routeState.params.slug = 'taller-1'
    mockedGetPostsBySection.mockReset()
    mockedRecordSectionView.mockReset()
    mockedRecordSectionView.mockResolvedValue()
  })

  it('shows loading while posts are requested', () => {
    mockedGetPostsBySection.mockReturnValue(new Promise(() => undefined))

    const wrapper = mount(SectionPlaceholderView, {
      props: { sectionKind: 'taller' },
      global: { stubs: { RouterLink: routerLinkStub } },
    })

    expect(wrapper.text()).toContain('Cargando publicaciones...')
  })

  it('shows an empty state when a section has no posts', async () => {
    mockedGetPostsBySection.mockResolvedValue(emptyResponse)

    const wrapper = mount(SectionPlaceholderView, {
      props: { sectionKind: 'taller' },
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(mockedGetPostsBySection).toHaveBeenCalledWith('taller-1')
    expect(mockedRecordSectionView).toHaveBeenCalledWith('taller-1')
    expect(wrapper.text()).toContain('Aún no hay publicaciones disponibles en esta sección.')
  })

  it('renders published posts and hashtags without showing content previews', async () => {
    mockedGetPostsBySection.mockResolvedValue({
      ...emptyResponse,
      posts: [
        {
          id: 10,
          title: 'Pregunta publicada',
          section: emptyResponse.section,
          tags: [{ name: 'Cuencas', slug: 'cuencas' }],
          publishedAt: '2026-01-02T00:00:00Z',
        },
      ],
    })

    const wrapper = mount(SectionPlaceholderView, {
      props: { sectionKind: 'taller' },
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Pregunta publicada')
    expect(wrapper.text()).toContain('#Cuencas')
    expect(wrapper.text()).toContain('Leer publicación')
    expect(wrapper.text()).not.toContain('Contenido de la solución')
  })

  it('shows a friendly error when posts fail', async () => {
    mockedGetPostsBySection.mockRejectedValue(new Error('Network error'))

    const wrapper = mount(SectionPlaceholderView, {
      props: { sectionKind: 'taller' },
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar las publicaciones de esta sección.')
    expect(mockedRecordSectionView).not.toHaveBeenCalled()
  })

  it('does not render posts when the route section type is incorrect', async () => {
    mockedGetPostsBySection.mockResolvedValue({
      section: {
        id: 4,
        type: 'PARCIAL',
        name: 'Parcial 1',
        slug: 'parcial-1',
        description: null,
      },
      posts: [
        {
          id: 20,
          title: 'Publicación de parcial',
          section: {
            id: 4,
            type: 'PARCIAL',
            name: 'Parcial 1',
            slug: 'parcial-1',
            description: null,
          },
          tags: [],
          publishedAt: '2026-01-02T00:00:00Z',
        },
      ],
    })

    const wrapper = mount(SectionPlaceholderView, {
      props: { sectionKind: 'taller' },
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar las publicaciones de esta sección.')
    expect(wrapper.text()).not.toContain('Publicación de parcial')
    expect(mockedRecordSectionView).not.toHaveBeenCalled()
  })
})
