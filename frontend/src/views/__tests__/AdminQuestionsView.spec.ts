import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { getPendingQuestions } from '@/services/api/adminService'
import { signOut } from '@/services/firebase/authService'
import type { AdminPendingQuestionsResponse, AdminQuestionSummary } from '@/types/adminQuestion'
import AdminQuestionsView from '@/views/admin/AdminQuestionsView.vue'

const routerPush = vi.hoisted(() => vi.fn<(route: unknown) => void>())

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRouter: () => ({ push: routerPush }),
  }
})

vi.mock('@/services/api/adminService', () => ({
  getPendingQuestions: vi.fn<
    (page?: number, size?: number) => Promise<AdminPendingQuestionsResponse>
  >(),
}))

vi.mock('@/services/api/adminErrors', () => ({
  isAdminAuthorizationError: vi.fn<(error: unknown) => boolean>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetPendingQuestions = vi.mocked(getPendingQuestions)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminQuestionsView', () => {
  beforeEach(() => {
    routerPush.mockReset()
    mockedGetPendingQuestions.mockReset()
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders loading state while pending questions are being fetched', () => {
    mockedGetPendingQuestions.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando preguntas pendientes...')
  })

  it('renders empty state when there are no pending questions', async () => {
    mockedGetPendingQuestions.mockResolvedValue(response({ items: [] }))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No hay preguntas pendientes.')
  })

  it('renders pending questions with nickname, anonymous fallback and attachment indicator', async () => {
    mockedGetPendingQuestions.mockResolvedValue(
      response({
        items: [
          summary({ id: 1, nickname: null, hasAttachment: true }),
          summary({ id: 2, nickname: 'Goca', hasAttachment: false }),
        ],
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Anónimo')
    expect(wrapper.text()).toContain('Goca')
    expect(wrapper.text()).toContain('Con imagen')
    expect(wrapper.text()).toContain('Ver pregunta')
    const links = wrapper.findAllComponents(RouterLinkStub)
    expect(links[0]?.props('to')).toEqual({
      name: 'admin-question-detail',
      params: { id: 1 },
    })
  })

  it('renders error state for non authorization failures', async () => {
    mockedGetPendingQuestions.mockRejectedValue(new Error('Network'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar las preguntas.')
  })

  it('signs out and redirects when admin authorization fails', async () => {
    mockedGetPendingQuestions.mockRejectedValue(new Error('Unauthorized'))
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
    mockedGetPendingQuestions
      .mockResolvedValueOnce(response({ page: 0, totalPages: 2 }))
      .mockResolvedValueOnce(response({ page: 1, totalPages: 2 }))
      .mockResolvedValueOnce(response({ page: 0, totalPages: 2 }))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('button:last-of-type').trigger('click')
    await flushPromises()

    expect(mockedGetPendingQuestions).toHaveBeenLastCalledWith(1, 20)
    expect(wrapper.text()).toContain('Página 2 de 2')

    await wrapper.get('button:first-of-type').trigger('click')
    await flushPromises()

    expect(mockedGetPendingQuestions).toHaveBeenLastCalledWith(0, 20)
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

function response(overrides: Partial<ReturnType<typeof baseResponse>> = {}) {
  return {
    ...baseResponse(),
    ...overrides,
  }
}

function baseResponse() {
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
    questionPreview: 'Pregunta enviada por estudiante',
    hasAttachment: false,
    createdAt: '2026-01-01T00:00:00Z',
  }
}
