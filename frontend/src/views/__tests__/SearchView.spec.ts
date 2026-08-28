import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { searchPosts } from '@/services/api/postService'
import type { PostSearchResult } from '@/types/post'
import SearchView from '@/views/posts/SearchView.vue'

vi.mock('@/services/api/postService', () => ({
  searchPosts: vi.fn<(query: string) => Promise<PostSearchResult[]>>(),
}))

const mockedSearchPosts = vi.mocked(searchPosts)

describe('SearchView', () => {
  beforeEach(() => {
    mockedSearchPosts.mockReset()
  })

  it('shows an invalid query state without calling the API', async () => {
    const { wrapper } = await mountView('/buscar?q=a')

    expect(wrapper.text()).toContain('La búsqueda debe tener al menos 2 caracteres.')
    expect(mockedSearchPosts).not.toHaveBeenCalled()
  })

  it('shows loading while search results are requested', async () => {
    mockedSearchPosts.mockReturnValue(new Promise(() => undefined))

    const { wrapper } = await mountView('/buscar?q=balance')

    expect(wrapper.text()).toContain('Buscando publicaciones...')
  })

  it('renders search results with section tags snippet and detail link', async () => {
    mockedSearchPosts.mockResolvedValue([postResult()])

    const { wrapper } = await mountView('/buscar?q=balance')
    await flushPromises()

    expect(mockedSearchPosts).toHaveBeenCalledWith('balance')
    expect(wrapper.text()).toContain('1 resultado para "balance"')
    expect(wrapper.text()).toContain('Balance hidrico')
    expect(wrapper.text()).toContain('Taller 1')
    expect(wrapper.text()).toContain('#Cuencas')
    expect(wrapper.text()).toContain('Extracto seguro de balance')
    expect(wrapper.get('a[href="/publicaciones/10"]').text()).toContain('Leer publicación')
    expect(wrapper.html()).not.toContain('<mark')
  })

  it('shows an empty state when no posts match', async () => {
    mockedSearchPosts.mockResolvedValue([])

    const { wrapper } = await mountView('/buscar?q=xyz')
    await flushPromises()

    expect(wrapper.text()).toContain('No encontramos publicaciones para "xyz".')
  })

  it('shows a friendly error when search fails', async () => {
    mockedSearchPosts.mockRejectedValue(new Error('Network'))

    const { wrapper } = await mountView('/buscar?q=balance')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos completar la búsqueda.')
    expect(wrapper.text()).not.toContain('Network')
  })

  it('updates results when the query string changes', async () => {
    mockedSearchPosts.mockResolvedValueOnce([postResult({ title: 'Balance' })])
    mockedSearchPosts.mockResolvedValueOnce([postResult({ title: 'Cuencas' })])
    const { wrapper, router } = await mountView('/buscar?q=balance')
    await flushPromises()

    await router.push('/buscar?q=cuenca')
    await flushPromises()

    expect(mockedSearchPosts).toHaveBeenNthCalledWith(1, 'balance')
    expect(mockedSearchPosts).toHaveBeenNthCalledWith(2, 'cuenca')
    expect(wrapper.text()).toContain('Cuencas')
    expect(wrapper.text()).not.toContain('1 resultado para "balance"')
  })
})

async function mountView(path: string): Promise<{ wrapper: ReturnType<typeof mount>; router: Router }> {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/buscar', name: 'search', component: SearchView },
      { path: '/publicaciones/:id', name: 'post-detail', component: { template: '<div />' } },
      { path: '/hashtags/:slug', name: 'hashtag-detail', component: { template: '<div />' } },
    ],
  })
  await router.push(path)
  await router.isReady()

  const wrapper = mount(SearchView, {
    global: {
      plugins: [router],
    },
  })

  return { wrapper, router }
}

function postResult(overrides: Partial<PostSearchResult> = {}): PostSearchResult {
  return {
    id: 10,
    title: 'Balance hidrico',
    section: {
      id: 1,
      type: 'TALLER',
      name: 'Taller 1',
      slug: 'taller-1',
      description: null,
    },
    tags: [{ name: 'Cuencas', slug: 'cuencas' }],
    snippet: 'Extracto seguro de balance',
    publishedAt: '2026-01-02T00:00:00Z',
    ...overrides,
  }
}
