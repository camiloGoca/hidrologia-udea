import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { createAdminPost, getPostsByStatus } from '@/services/api/adminPostService'
import { getSections } from '@/services/api/sectionService'
import { signOut } from '@/services/firebase/authService'
import type { AdminPost, AdminPostStatus, AdminPostSummary, AdminPostsResponse } from '@/types/adminPost'
import type { Section } from '@/types/section'
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
  createAdminPost: vi.fn<(payload: { sectionSlug: string }) => Promise<AdminPost>>(),
}))

vi.mock('@/services/api/sectionService', () => ({
  getSections: vi.fn<() => Promise<Section[]>>(),
}))

vi.mock('@/services/api/adminErrors', () => ({
  isAdminAuthorizationError: vi.fn<(error: unknown) => boolean>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetPostsByStatus = vi.mocked(getPostsByStatus)
const mockedCreateAdminPost = vi.mocked(createAdminPost)
const mockedGetSections = vi.mocked(getSections)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminPostsView', () => {
  beforeEach(() => {
    routeQuery.estado = undefined
    routerPush.mockReset()
    routerReplace.mockReset()
    mockedGetPostsByStatus.mockReset()
    mockedCreateAdminPost.mockReset()
    mockedGetSections.mockReset()
    mockedGetSections.mockResolvedValue(sections())
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
    expect(wrapper.text()).toContain('Nueva publicación')

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

    await buttonByText(wrapper, 'Siguiente').trigger('click')
    await flushPromises()

    expect(mockedGetPostsByStatus).toHaveBeenLastCalledWith('DRAFT', 1, 20)
    expect(wrapper.text()).toContain('Página 2 de 2')

    await buttonByText(wrapper, 'Anterior').trigger('click')
    await flushPromises()

    expect(mockedGetPostsByStatus).toHaveBeenLastCalledWith('DRAFT', 0, 20)
  })

  it('opens create modal and loads sections from the backend service', async () => {
    mockedGetPostsByStatus.mockResolvedValue(response({ items: [] }))

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Nueva publicación').trigger('click')
    await flushPromises()

    expect(mockedGetSections).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Crear borrador')
    expect(wrapper.text()).toContain('Taller 1')
    expect(wrapper.text()).toContain('Parcial 1')
    expect((wrapper.find('select').element as HTMLSelectElement).value).toBe('taller-1')
  })

  it('creates a manual draft and navigates to the existing editor', async () => {
    mockedGetPostsByStatus.mockResolvedValue(response({ items: [] }))
    let resolveCreate!: (post: AdminPost) => void
    mockedCreateAdminPost.mockReturnValue(
      new Promise((resolve) => {
        resolveCreate = resolve
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Nueva publicación').trigger('click')
    await flushPromises()
    await wrapper.find('select').setValue('parcial-1')
    await wrapper.find('form').trigger('submit')
    await wrapper.find('form').trigger('submit')

    resolveCreate(adminPost({ id: 10 }))
    await flushPromises()

    expect(mockedCreateAdminPost).toHaveBeenCalledTimes(1)
    expect(mockedCreateAdminPost).toHaveBeenCalledWith({ sectionSlug: 'parcial-1' })
    expect(routerPush).toHaveBeenCalledWith({
      name: 'admin-post-detail',
      params: { id: 10 },
    })
  })

  it('shows a friendly create error without technical details', async () => {
    mockedGetPostsByStatus.mockResolvedValue(response({ items: [] }))
    mockedCreateAdminPost.mockRejectedValue(new Error('SQL detail'))

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Nueva publicación').trigger('click')
    await flushPromises()
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos crear el borrador.')
    expect(wrapper.text()).not.toContain('SQL detail')
  })

  it('redirects on authorization error while creating a draft', async () => {
    mockedGetPostsByStatus.mockResolvedValue(response({ items: [] }))
    mockedCreateAdminPost.mockRejectedValue(new Error('Unauthorized'))
    mockedIsAdminAuthorizationError.mockReturnValue(true)

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Nueva publicación').trigger('click')
    await flushPromises()
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedSignOut).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith({
      name: 'admin-login',
      query: { reason: 'forbidden' },
    })
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

function buttonByText(wrapper: ReturnType<typeof mountView>, text: string) {
  const button = wrapper.findAll('button').find((candidate) => candidate.text() === text)
  if (!button) {
    throw new Error(`Button not found: ${text}`)
  }

  return button
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

function adminPost(overrides: Partial<AdminPost> = {}): AdminPost {
  return {
    id: 10,
    title: '',
    content: '',
    status: 'DRAFT',
    sourceQuestionId: null,
    section: sections()[0]!,
    sourceQuestion: null,
    tags: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    publishedAt: null,
    ...overrides,
  }
}

function sections(): Section[] {
  return [
    {
      id: 1,
      type: 'TALLER',
      name: 'Taller 1',
      slug: 'taller-1',
      description: null,
      displayOrder: 1,
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
}
