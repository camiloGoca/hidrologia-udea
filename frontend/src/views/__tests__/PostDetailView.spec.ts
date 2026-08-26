import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getPostById } from '@/services/api/postService'
import type { PostDetail } from '@/types/post'
import PostDetailView from '@/views/posts/PostDetailView.vue'

const routeState = vi.hoisted(() => ({
  params: {
    id: '10',
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
  getPostById: vi.fn<() => Promise<PostDetail>>(),
}))

const mockedGetPostById = vi.mocked(getPostById)

const routerLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a><slot /></a>',
}

const post: PostDetail = {
  id: 10,
  title: 'Pregunta publicada',
  content: 'Linea 1\nLinea 2',
  contentDocument: {
    type: 'doc',
    content: [
      {
        type: 'paragraph',
        content: [
          { type: 'text', text: 'Linea 1' },
          { type: 'hardBreak' },
          { type: 'text', text: 'Linea 2' },
        ],
      },
    ],
  },
  section: {
    id: 1,
    type: 'TALLER',
    name: 'Taller 1',
    slug: 'taller-1',
    description: 'Morfometría de cuencas',
  },
  tags: [{ name: 'Cuencas', slug: 'cuencas' }],
  publishedAt: '2026-01-02T00:00:00Z',
}

describe('post detail view', () => {
  beforeEach(() => {
    routeState.params.id = '10'
    mockedGetPostById.mockReset()
  })

  it('renders a published post detail with section and hashtags', async () => {
    mockedGetPostById.mockResolvedValue(post)

    const wrapper = mount(PostDetailView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(mockedGetPostById).toHaveBeenCalledWith('10')
    expect(wrapper.text()).toContain('Pregunta publicada')
    expect(wrapper.text()).toContain('Taller 1')
    expect(wrapper.text()).toContain('Linea 1')
    expect(wrapper.text()).toContain('Linea 2')
    expect(wrapper.text()).toContain('#Cuencas')
  })

  it('shows an error when the post is unavailable', async () => {
    mockedGetPostById.mockRejectedValue(new Error('Not found'))

    const wrapper = mount(PostDetailView, {
      global: { stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar esta publicación.')
  })
})
