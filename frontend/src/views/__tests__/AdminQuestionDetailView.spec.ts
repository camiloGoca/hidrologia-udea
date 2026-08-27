import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import {
  archiveQuestion,
  createQuestionDraft,
  discardQuestionDraft,
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
  createQuestionDraft: vi.fn<(id: number) => Promise<unknown>>(),
  discardQuestionDraft: vi.fn<(id: number) => Promise<void>>(),
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
const mockedCreateQuestionDraft = vi.mocked(createQuestionDraft)
const mockedDiscardQuestionDraft = vi.mocked(discardQuestionDraft)
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
    mockedCreateQuestionDraft.mockReset()
    mockedDiscardQuestionDraft.mockReset()
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
    expect(wrapper.text()).toContain('Crear borrador de publicación')
    expect(wrapper.text()).toContain('Archivar')
    expect(wrapper.text()).toContain('Rechazar')
    expect(wrapper.text()).not.toContain('Reabrir')
  })

  it('creates a draft and navigates to the admin post detail', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ status: 'PENDING' }))
    mockedCreateQuestionDraft.mockResolvedValue({
      id: 9,
      status: 'DRAFT',
      title: '',
      content: '',
      contentDocument: {
        type: 'doc',
        content: [{ type: 'paragraph' }],
      },
      sourceQuestionId: 1,
      sourceQuestion: null,
      section: baseDetail().section,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
      publishedAt: null,
      tags: [],
      images: [],
    })

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Crear borrador de publicación').trigger('click')
    expect(wrapper.text()).toContain('¿Crear un borrador a partir de esta pregunta?')
    await buttonByText(wrapper, 'Crear borrador').trigger('click')
    await flushPromises()

    expect(mockedCreateQuestionDraft).toHaveBeenCalledWith(1)
    expect(routerPush).toHaveBeenCalledWith({ name: 'admin-post-detail', params: { id: 9 } })
  })

  it('shows linked draft actions and hides archive and reject', async () => {
    mockedGetQuestionById.mockResolvedValue(
      detail({
        status: 'PENDING',
        linkedPost: { id: 9, status: 'DRAFT', title: '' },
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Borrador en preparación')
    expect(wrapper.text()).toContain('Ver borrador')
    expect(wrapper.text()).toContain('Descartar borrador')
    expect(wrapper.text()).not.toContain('Archivar')
    expect(wrapper.text()).not.toContain('Rechazar')
  })

  it('discards a linked draft and keeps the question pending', async () => {
    mockedGetQuestionById.mockResolvedValue(
      detail({
        status: 'PENDING',
        linkedPost: { id: 9, status: 'DRAFT', title: '' },
      }),
    )
    mockedDiscardQuestionDraft.mockResolvedValue()

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Descartar borrador').trigger('click')
    expect(wrapper.text()).toContain('¿Descartar este borrador?')
    await lastButtonByText(wrapper, 'Descartar borrador').trigger('click')
    await flushPromises()

    expect(mockedDiscardQuestionDraft).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('El borrador fue descartado.')
    expect(wrapper.text()).toContain('Archivar')
    expect(wrapper.text()).toContain('Rechazar')
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
    mockedGetQuestionById.mockResolvedValue(
      detail({
        status: 'PUBLISHED',
        linkedPost: { id: 9, status: 'PUBLISHED', title: 'Factor de forma de una cuenca' },
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('PUBLICADA')
    expect(wrapper.text()).toContain('Publicación asociada')
    expect(wrapper.text()).toContain('Factor de forma de una cuenca')
    expect(wrapper.text()).toContain('Ver publicación')
    expect(wrapper.text()).toContain('Ver en administración')
    expect(wrapper.text()).not.toContain('Archivar')
    expect(wrapper.text()).not.toContain('Rechazar')
    expect(wrapper.text()).not.toContain('Reabrir')

    const links = wrapper.findAllComponents(RouterLinkStub)
    expect(links.some((link) => linkToObject(link.props('to')).name === 'post-detail')).toBe(true)
    expect(links.some((link) => linkToObject(link.props('to')).name === 'admin-post-detail')).toBe(
      true,
    )
  })

  it('shows archived linked post without public post link', async () => {
    mockedGetQuestionById.mockResolvedValue(
      detail({
        status: 'PUBLISHED',
        linkedPost: { id: 9, status: 'ARCHIVED', title: 'Factor de forma de una cuenca' },
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('PUBLICADA')
    expect(wrapper.text()).toContain('Publicación asociada')
    expect(wrapper.text()).toContain('Estado: ARCHIVADA')
    expect(wrapper.text()).toContain('Ver en administración')
    expect(wrapper.text()).not.toContain('Ver publicación')

    const links = wrapper.findAllComponents(RouterLinkStub)
    expect(links.some((link) => linkToObject(link.props('to')).name === 'post-detail')).toBe(false)
    expect(links.some((link) => linkToObject(link.props('to')).name === 'admin-post-detail')).toBe(
      true,
    )
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

function lastButtonByText(wrapper: ReturnType<typeof mountView>, text: string) {
  const buttons = wrapper.findAll('button').filter((candidate) => candidate.text() === text)
  const button = buttons[buttons.length - 1]
  if (!button) {
    throw new Error(`Button not found: ${text}`)
  }

  return button
}

function linkToObject(to: string | Record<string, unknown>): { name?: string } {
  if (typeof to === 'string') {
    return {}
  }

  return to as { name?: string }
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
    linkedPost: null,
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
