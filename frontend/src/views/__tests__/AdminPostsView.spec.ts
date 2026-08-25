import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { getPostsByStatus } from '@/services/api/adminPostService'
import { signOut } from '@/services/firebase/authService'
import type { AdminPostStatus, AdminPostSummary, AdminPostsResponse } from '@/types/adminPost'
import AdminPostsView from '@/views/admin/AdminPostsView.vue'

const routerPush = vi.hoisted(() => vi.fn<(route: unknown) => void>())
const routerReplace = vi.hoisted(() => vi.fn<(route: unknown) => void>())
const routeQuery = vi.hoisted(() => ({ estado: undefined as string | undefined }))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRoute: () => ({ query: routeQuery }),
    useRouter: () => ({ push: routerPush, replace: routerReplace }),
  }
})

vi.mock('@/services/api/adminPostService', () => ({
  getPostsByStatus: vi.fn<
    (status: AdminPostStatus, page?: number, size?: number) => Promise<AdminPostsResponse>
  >(),
}))

vi.mock('@/services/api/adminErrors', () => ({
  isAdminAuthorizationError: vi.fn<(error: unknown) => boolean>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetPostsByStatus = vi.mocked(getPostsByStatus)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminPostsView', () => {
  beforeEach(() => {
    routeQuery.estado = undefined
    routerPush.mockReset()
    routerReplace.mockReset()
    mockedGetPostsByStatus.mockReset()
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders loading state while drafts are being fetched', () => {
    mockedGetPostsByStatus.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando borradores...')
  })

  it('renders tabs using URL state', async () => {
    mockedGetPostsByStatus.mockResolvedValue(response({ items: [] }))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Borradores')
    expect(wrapper.text()).toContain('Publicadas')
    expect(wrapper.text()).toContain('Archivadas')

    const links = wrapper.findAllComponents(RouterLinkStub)
    expect(links.some((link) => linkToObject(link.props('to')).query?.estado === 'borradores')).toBe(
      true,
    )
    expect(links.some((link) => linkToObject(link.props('to')).query?.estado === 'publicadas')).toBe(
      true,
    )
    expect(links.some((link) => linkToObject(link.props('to')).query?.estado === 'archivadas')).toBe(
      true,
    )
  })

  it('normalizes invalid query param to drafts tab', async () => {
    routeQuery.estado = 'desconocidas'
    mockedGetPostsByStatus.mockResolvedValue(response({ items: [] }))

    mountView()
    await flushPromises()

    expect(routerReplace).toHaveBeenCalledWith({
      name: 'admin-posts',
      query: { estado: 'borradores' },
    })
    expect(mockedGetPostsByStatus).not.toHaveBeenCalled()
  })

  it('loads published posts and renders translated badges', async () => {
    routeQuery.estado = 'publicadas'
    mockedGetPostsByStatus.mockResolvedValue(
      response({
        items: [summary({ status: 'PUBLISHED', title: 'Publicación visible' })],
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(mockedGetPostsByStatus).toHaveBeenCalledWith('PUBLISHED', 0, 20)
    expect(wrapper.text()).toContain('PUBLICADA')
    expect(wrapper.text()).toContain('Publicación visible')
    expect(wrapper.text()).toContain('Publicado')
  })

  it('renders archived empty state', async () => {
    routeQuery.estado = 'archivadas'
    mockedGetPostsByStatus.mockResolvedValue(response({ items: [] }))

    const wrapper = mountView()
    await flushPromises()

    expect(mockedGetPostsByStatus).toHaveBeenCalledWith('ARCHIVED', 0, 20)
    expect(wrapper.text()).toContain('No hay publicaciones archivadas.')
  })

  it('renders draft with empty title fallback and source question indicator', async () => {
    mockedGetPostsByStatus.mockResolvedValue(
      response({
        items: [summary({ title: '', status: 'DRAFT', hasSourceQuestion: true })],
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('BORRADOR')
    expect(wrapper.text()).toContain('Sin título')
    expect(wrapper.text()).toContain('Nació de una pregunta')
    const links = wrapper.findAllComponents(RouterLinkStub)
    expect(links[links.length - 1]?.props('to')).toEqual({
      name: 'admin-post-detail',
      params: { id: 9 },
    })
  })

  it('renders error state for non authorization failures', async () => {
    mockedGetPostsByStatus.mockRejectedValue(new Error('Network'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar las publicaciones.')
  })

  it('signs out and redirects when admin authorization fails', async () => {
    mockedGetPostsByStatus.mockRejectedValue(new Error('Unauthorized'))
    mockedIsAdminAuthorizationError.mockReturnValue(true)

    mountView()
    await flushPromises()

    expect(mockedSignOut).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith({
      name: 'admin-login',
      query: { reason: 'forbidden' },
    })
  })

  it('loads next and previous pages through backend pagination', async () => {
    mockedGetPostsByStatus
      .mockResolvedValueOnce(response({ page: 0, totalPages: 2 }))
      .mockResolvedValueOnce(response({ page: 1, totalPages: 2 }))
      .mockResolvedValueOnce(response({ page: 0, totalPages: 2 }))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('button:last-of-type').trigger('click')
    await flushPromises()

    expect(mockedGetPostsByStatus).toHaveBeenLastCalledWith('DRAFT', 1, 20)
    expect(wrapper.text()).toContain('Página 2 de 2')

    await wrapper.get('button:first-of-type').trigger('click')
    await flushPromises()

    expect(mockedGetPostsByStatus).toHaveBeenLastCalledWith('DRAFT', 0, 20)
  })
})

function mountView() {
  return mount(AdminPostsView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })
}

function linkToObject(to: string | Record<string, unknown>): { query?: Record<string, unknown> } {
  if (typeof to === 'string') {
    return {}
  }

  return to as { query?: Record<string, unknown> }
}

function response(overrides: Partial<AdminPostsResponse> = {}): AdminPostsResponse {
  return {
    items: [summary()],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    ...overrides,
  }
}

function summary(overrides: Partial<AdminPostSummary> = {}): AdminPostSummary {
  return {
    id: 9,
    title: 'Publicación',
    status: 'DRAFT',
    section: {
      id: 1,
      type: 'TALLER',
      name: 'Taller 1',
      slug: 'taller-1',
      description: null,
    },
    hasSourceQuestion: false,
    sourceQuestionId: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-02T00:00:00Z',
    publishedAt: null,
    ...overrides,
  }
}
