import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getPostsByTag } from '@/services/api/postService'
import type { TagPostsResponse } from '@/types/post'
import HashtagPostsView from '@/views/tags/HashtagPostsView.vue'

const routeState = vi.hoisted(() => ({
  params: {
    slug: 'cuencas',
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
  getPostsByTag: vi.fn<() => Promise<TagPostsResponse>>(),
}))

const mockedGetPostsByTag = vi.mocked(getPostsByTag)

const routerLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a><slot /></a>',
}

const emptyResponse: TagPostsResponse = {
  tag: { name: 'Cuencas', slug: 'cuencas' },
  posts: [],
}

describe('hashtag posts view', () => {
  beforeEach(() => {
    routeState.params.slug = 'cuencas'
    mockedGetPostsByTag.mockReset()
  })

  it('shows an empty state when a hashtag has no posts', async () => {
    mockedGetPostsByTag.mockResolvedValue(emptyResponse)

    const wrapper = mount(HashtagPostsView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(mockedGetPostsByTag).toHaveBeenCalledWith('cuencas')
    expect(wrapper.text()).toContain('#Cuencas')
    expect(wrapper.text()).toContain('Aún no hay publicaciones asociadas a este hashtag.')
  })

  it('renders published posts associated with a hashtag', async () => {
    mockedGetPostsByTag.mockResolvedValue({
      ...emptyResponse,
      posts: [
        {
          id: 10,
          title: 'Pregunta publicada',
          section: {
            id: 1,
            type: 'TALLER',
            name: 'Taller 1',
            slug: 'taller-1',
            description: 'Morfometría de cuencas',
          },
          tags: [{ name: 'Cuencas', slug: 'cuencas' }],
          publishedAt: '2026-01-02T00:00:00Z',
        },
      ],
    })

    const wrapper = mount(HashtagPostsView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Pregunta publicada')
    expect(wrapper.text()).toContain('Leer publicación')
  })

  it('shows an error when the hashtag is unavailable', async () => {
    mockedGetPostsByTag.mockRejectedValue(new Error('Not found'))

    const wrapper = mount(HashtagPostsView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar este hashtag.')
  })
})
