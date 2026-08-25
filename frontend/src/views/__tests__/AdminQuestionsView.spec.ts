import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { getQuestionsByStatus } from '@/services/api/adminService'
import { signOut } from '@/services/firebase/authService'
import type {
  AdminQuestionStatus,
  AdminQuestionSummary,
  AdminQuestionsResponse,
} from '@/types/adminQuestion'
import AdminQuestionsView from '@/views/admin/AdminQuestionsView.vue'

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

vi.mock('@/services/api/adminService', () => ({
  getQuestionsByStatus: vi.fn<
    (status: Exclude<AdminQuestionStatus, 'PUBLISHED'>, page?: number, size?: number) => Promise<AdminQuestionsResponse>
  >(),
}))

vi.mock('@/services/api/adminErrors', () => ({
  isAdminAuthorizationError: vi.fn<(error: unknown) => boolean>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetQuestionsByStatus = vi.mocked(getQuestionsByStatus)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminQuestionsView', () => {
  beforeEach(() => {
    routeQuery.estado = undefined
    routerPush.mockReset()
    routerReplace.mockReset()
    mockedGetQuestionsByStatus.mockReset()
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders loading state while pending questions are being fetched', () => {
    mockedGetQuestionsByStatus.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando preguntas pendientes...')
  })

  it('renders accessible tabs with URL state', async () => {
    mockedGetQuestionsByStatus.mockResolvedValue(response({ items: [] }))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Pendientes')
    expect(wrapper.text()).toContain('Archivadas')
    expect(wrapper.text()).toContain('Rechazadas')

    const links = wrapper.findAllComponents(RouterLinkStub)
    expect(links.some((link) => linkToObject(link.props('to')).query?.estado === 'archivadas')).toBe(
      true,
    )
    expect(links.some((link) => linkToObject(link.props('to')).query?.estado === 'rechazadas')).toBe(
      true,
    )
  })

  it('normalizes invalid query param to pending tab', async () => {
    routeQuery.estado = 'desconocidas'
    mockedGetQuestionsByStatus.mockResolvedValue(response({ items: [] }))

    mountView()
    await flushPromises()

    expect(routerReplace).toHaveBeenCalledWith({
      name: 'admin-questions',
      query: { estado: 'pendientes' },
    })
    expect(mockedGetQuestionsByStatus).not.toHaveBeenCalled()
  })

  it('loads archived questions when the archived tab is active', async () => {
    routeQuery.estado = 'archivadas'
    mockedGetQuestionsByStatus.mockResolvedValue(response({ items: [] }))

    const wrapper = mountView()
    await flushPromises()

    expect(mockedGetQuestionsByStatus).toHaveBeenCalledWith('ARCHIVED', 0, 20)
    expect(wrapper.text()).toContain('No hay preguntas archivadas.')
  })

  it('loads rejected questions and renders translated badges', async () => {
    routeQuery.estado = 'rechazadas'
    mockedGetQuestionsByStatus.mockResolvedValue(
      response({
        items: [summary({ status: 'REJECTED', nickname: 'Goca', hasAttachment: true })],
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(mockedGetQuestionsByStatus).toHaveBeenCalledWith('REJECTED', 0, 20)
    expect(wrapper.text()).toContain('RECHAZADA')
    expect(wrapper.text()).toContain('Goca')
    expect(wrapper.text()).toContain('Con imagen')
  })

  it('renders pending questions with anonymous fallback and detail link preserving tab', async () => {
    mockedGetQuestionsByStatus.mockResolvedValue(
      response({
        items: [summary({ id: 1, nickname: null, status: 'PENDING', hasLinkedPost: true })],
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Anónimo')
    expect(wrapper.text()).toContain('PENDIENTE')
    expect(wrapper.text()).toContain('Borrador en preparación')
    expect(wrapper.text()).toContain('Ver pregunta')
    const links = wrapper.findAllComponents(RouterLinkStub)
    expect(links[links.length - 1]?.props('to')).toEqual({
      name: 'admin-question-detail',
      params: { id: 1 },
      query: { estado: 'pendientes' },
    })
  })

  it('renders each status empty state', async () => {
    routeQuery.estado = 'rechazadas'
    mockedGetQuestionsByStatus.mockResolvedValue(response({ items: [] }))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No hay preguntas rechazadas.')
  })

  it('renders error state for non authorization failures', async () => {
    mockedGetQuestionsByStatus.mockRejectedValue(new Error('Network'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar las preguntas.')
  })

  it('signs out and redirects when admin authorization fails', async () => {
    mockedGetQuestionsByStatus.mockRejectedValue(new Error('Unauthorized'))
    mockedIsAdminAuthorizationError.mockReturnValue(true)

    mountView()
    await flushPromises()

    expect(mockedSignOut).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith({
      name: 'admin-login',
      query: { reason: 'forbidden' },
    })
  })

  it('loads the next and previous pages through backend pagination', async () => {
    mockedGetQuestionsByStatus
      .mockResolvedValueOnce(response({ page: 0, totalPages: 2 }))
      .mockResolvedValueOnce(response({ page: 1, totalPages: 2 }))
      .mockResolvedValueOnce(response({ page: 0, totalPages: 2 }))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('button:last-of-type').trigger('click')
    await flushPromises()

    expect(mockedGetQuestionsByStatus).toHaveBeenLastCalledWith('PENDING', 1, 20)
    expect(wrapper.text()).toContain('Página 2 de 2')

    await wrapper.get('button:first-of-type').trigger('click')
    await flushPromises()

    expect(mockedGetQuestionsByStatus).toHaveBeenLastCalledWith('PENDING', 0, 20)
  })
})

function mountView() {
  return mount(AdminQuestionsView, {
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

function response(overrides: Partial<AdminQuestionsResponse> = {}): AdminQuestionsResponse {
  return {
    ...baseResponse(),
    ...overrides,
  }
}

function baseResponse(): AdminQuestionsResponse {
  return {
    items: [summary()],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  }
}

function summary(overrides: Partial<AdminQuestionSummary> = {}): AdminQuestionSummary {
  return {
    ...baseSummary(),
    ...overrides,
  }
}

function baseSummary(): AdminQuestionSummary {
  return {
    id: 1,
    nickname: null,
    section: {
      id: 1,
      type: 'TALLER' as const,
      name: 'Taller 1',
      slug: 'taller-1',
      description: null,
    },
    status: 'PENDING',
    questionPreview: 'Pregunta enviada por estudiante',
    hasAttachment: false,
    hasLinkedPost: false,
    createdAt: '2026-01-01T00:00:00Z',
  }
}
