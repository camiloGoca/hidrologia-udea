import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import {
  archiveQuestion,
  getQuestionById,
  rejectQuestion,
  reopenQuestion,
} from '@/services/api/adminService'
import { signOut } from '@/services/firebase/authService'
import type { AdminQuestionDetail, AdminQuestionStatusUpdateResponse } from '@/types/adminQuestion'
import AdminQuestionDetailView from '@/views/admin/AdminQuestionDetailView.vue'

const routerPush = vi.hoisted(() => vi.fn<(route: unknown) => void>())
const routeParams = vi.hoisted(() => ({ id: '1' as string | string[] }))
const routeQuery = vi.hoisted(() => ({ estado: 'pendientes' as string | undefined }))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRoute: () => ({ params: routeParams, query: routeQuery }),
    useRouter: () => ({ push: routerPush }),
  }
})

vi.mock('@/services/api/adminService', () => ({
  getQuestionById: vi.fn<(id: number) => Promise<AdminQuestionDetail>>(),
  rejectQuestion: vi.fn<(id: number) => Promise<AdminQuestionStatusUpdateResponse>>(),
  archiveQuestion: vi.fn<(id: number) => Promise<AdminQuestionStatusUpdateResponse>>(),
  reopenQuestion: vi.fn<(id: number) => Promise<AdminQuestionStatusUpdateResponse>>(),
}))

vi.mock('@/services/api/adminErrors', () => ({
  isAdminAuthorizationError: vi.fn<(error: unknown) => boolean>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetQuestionById = vi.mocked(getQuestionById)
const mockedRejectQuestion = vi.mocked(rejectQuestion)
const mockedArchiveQuestion = vi.mocked(archiveQuestion)
const mockedReopenQuestion = vi.mocked(reopenQuestion)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminQuestionDetailView', () => {
  beforeEach(() => {
    routeParams.id = '1'
    routeQuery.estado = 'pendientes'
    routerPush.mockReset()
    mockedGetQuestionById.mockReset()
    mockedRejectQuestion.mockReset()
    mockedArchiveQuestion.mockReset()
    mockedReopenQuestion.mockReset()
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders loading state while the question is being fetched', () => {
    mockedGetQuestionById.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando pregunta...')
  })

  it('renders detail without image and keeps question as text', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ question: '<strong>No HTML</strong>' }))

    const wrapper = mountView()
    await flushPromises()

    expect(mockedGetQuestionById).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('Anónimo')
    expect(wrapper.text()).toContain('<strong>No HTML</strong>')
    expect(wrapper.find('strong').exists()).toBe(false)
    expect(wrapper.find('img').exists()).toBe(false)
  })

  it('renders detail with image and secure external link', async () => {
    mockedGetQuestionById.mockResolvedValue(
      detail({
        nickname: 'Goca',
        attachment: {
          secureUrl: 'https://res.cloudinary.com/demo/image/upload/question.png',
          format: 'png',
          width: 640,
          height: 480,
          bytes: 1000,
        },
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Goca')
    expect(wrapper.get('img').attributes('alt')).toBe('Imagen adjunta a la pregunta')
    expect(wrapper.get('img').attributes('src')).toBe(
      'https://res.cloudinary.com/demo/image/upload/question.png',
    )
    expect(wrapper.get('a[target="_blank"]').attributes('rel')).toBe('noopener noreferrer')
  })

  it('shows archive and reject actions for pending questions', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ status: 'PENDING' }))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('PENDIENTE')
    expect(wrapper.text()).toContain('Archivar')
    expect(wrapper.text()).toContain('Rechazar')
    expect(wrapper.text()).not.toContain('Reabrir')
  })

  it('shows reopen action for archived questions', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ status: 'ARCHIVED' }))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('ARCHIVADA')
    expect(wrapper.text()).toContain('Reabrir')
    expect(wrapper.text()).not.toContain('Rechazar')
  })

  it('shows reopen action for rejected questions', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ status: 'REJECTED' }))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('RECHAZADA')
    expect(wrapper.text()).toContain('Reabrir')
  })

  it('does not show V2A actions for published questions', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ status: 'PUBLISHED' }))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('PUBLICADA')
    expect(wrapper.text()).not.toContain('Archivar')
    expect(wrapper.text()).not.toContain('Rechazar')
    expect(wrapper.text()).not.toContain('Reabrir')
  })

  it('opens and cancels reject confirmation without calling API', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ status: 'PENDING' }))

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Rechazar').trigger('click')
    expect(wrapper.text()).toContain('¿Rechazar esta pregunta?')

    await buttonByText(wrapper, 'Cancelar').trigger('click')

    expect(mockedRejectQuestion).not.toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('¿Rechazar esta pregunta?')
  })

  it('confirms archive and updates status without leaving detail', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ status: 'PENDING' }))
    mockedArchiveQuestion.mockResolvedValue(updateResponse({ status: 'ARCHIVED' }))

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Archivar').trigger('click')
    expect(wrapper.text()).toContain('¿Archivar esta pregunta?')
    await buttonByText(wrapper, 'Archivar pregunta').trigger('click')
    await flushPromises()

    expect(mockedArchiveQuestion).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('ARCHIVADA')
    expect(wrapper.text()).toContain('La pregunta fue archivada.')
    expect(wrapper.text()).toContain('Reabrir')
  })

  it('confirms reopen and updates status', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ status: 'REJECTED' }))
    mockedReopenQuestion.mockResolvedValue(updateResponse({ status: 'PENDING' }))

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Reabrir').trigger('click')
    expect(wrapper.text()).toContain('¿Reabrir esta pregunta?')
    await buttonByText(wrapper, 'Reabrir pregunta').trigger('click')
    await flushPromises()

    expect(mockedReopenQuestion).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('PENDIENTE')
  })

  it('prevents duplicate action submit while request is pending', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ status: 'PENDING' }))
    mockedRejectQuestion.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Rechazar').trigger('click')
    await buttonByText(wrapper, 'Rechazar pregunta').trigger('click')
    await buttonByText(wrapper, 'Procesando...').trigger('click')

    expect(mockedRejectQuestion).toHaveBeenCalledTimes(1)
  })

  it('renders friendly action error without technical details', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ status: 'PENDING' }))
    mockedRejectQuestion.mockRejectedValue(new Error('SQL timeout'))

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Rechazar').trigger('click')
    await buttonByText(wrapper, 'Rechazar pregunta').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos completar la acción.')
    expect(wrapper.text()).not.toContain('SQL timeout')
  })

  it('renders error state for missing questions', async () => {
    mockedGetQuestionById.mockRejectedValue(new Error('Not found'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar esta pregunta.')
  })

  it('signs out and redirects when admin authorization fails', async () => {
    mockedGetQuestionById.mockRejectedValue(new Error('Unauthorized'))
    mockedIsAdminAuthorizationError.mockReturnValue(true)

    mountView()
    await flushPromises()

    expect(mockedSignOut).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith({
      name: 'admin-login',
      query: { reason: 'forbidden' },
    })
  })
})

function mountView() {
  return mount(AdminQuestionDetailView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })
}

function buttonByText(wrapper: ReturnType<typeof mountView>, text: string) {
  const button = wrapper.findAll('button').find((candidate) => candidate.text() === text)
  if (!button) {
    throw new Error(`Button not found: ${text}`)
  }

  return button
}

function detail(overrides: Partial<AdminQuestionDetail> = {}): AdminQuestionDetail {
  return {
    ...baseDetail(),
    ...overrides,
  }
}

function baseDetail(): AdminQuestionDetail {
  return {
    id: 1,
    nickname: null,
    question: 'Pregunta completa',
    status: 'PENDING' as const,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:30Z',
    section: {
      id: 1,
      type: 'TALLER' as const,
      name: 'Taller 1',
      slug: 'taller-1',
      description: null,
    },
    attachment: null,
  }
}

function updateResponse(
  overrides: Partial<AdminQuestionStatusUpdateResponse> = {},
): AdminQuestionStatusUpdateResponse {
  return {
    id: 1,
    status: 'REJECTED',
    updatedAt: '2026-01-01T00:01:00Z',
    ...overrides,
  }
}
