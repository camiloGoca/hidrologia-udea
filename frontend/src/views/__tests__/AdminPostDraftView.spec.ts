import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { discardQuestionDraft, getAdminPost } from '@/services/api/adminService'
import { signOut } from '@/services/firebase/authService'
import type { AdminPost } from '@/types/adminPost'
import AdminPostDraftView from '@/views/admin/AdminPostDraftView.vue'

const routerPush = vi.hoisted(() => vi.fn<(route: unknown) => void>())
const routeParams = vi.hoisted(() => ({ id: '9' as string | string[] }))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRoute: () => ({ params: routeParams }),
    useRouter: () => ({ push: routerPush }),
  }
})

vi.mock('@/services/api/adminService', () => ({
  getAdminPost: vi.fn<(id: number) => Promise<AdminPost>>(),
  discardQuestionDraft: vi.fn<(questionId: number) => Promise<void>>(),
}))

vi.mock('@/services/api/adminErrors', () => ({
  isAdminAuthorizationError: vi.fn<(error: unknown) => boolean>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetAdminPost = vi.mocked(getAdminPost)
const mockedDiscardQuestionDraft = vi.mocked(discardQuestionDraft)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminPostDraftView', () => {
  beforeEach(() => {
    routeParams.id = '9'
    routerPush.mockReset()
    mockedGetAdminPost.mockReset()
    mockedDiscardQuestionDraft.mockReset()
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders loading state while the draft is being fetched', () => {
    mockedGetAdminPost.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando borrador...')
  })

  it('renders draft metadata and source question without editable controls', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost())

    const wrapper = mountView()
    await flushPromises()

    expect(mockedGetAdminPost).toHaveBeenCalledWith(9)
    expect(wrapper.text()).toContain('Borrador de publicación')
    expect(wrapper.text()).toContain('BORRADOR')
    expect(wrapper.text()).toContain('Sin título')
    expect(wrapper.text()).toContain('Sin contenido todavía')
    expect(wrapper.text()).toContain('Anónimo')
    expect(wrapper.text()).toContain('Pregunta de origen')
    expect(wrapper.text()).toContain('La pregunta original tiene una imagen adjunta privada.')
    expect(wrapper.find('input').exists()).toBe(false)
    expect(wrapper.find('textarea').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Publicar')
  })

  it('opens and cancels discard confirmation without calling API', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost())

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Descartar borrador').trigger('click')
    expect(wrapper.text()).toContain('¿Descartar este borrador?')

    await buttonByText(wrapper, 'Cancelar').trigger('click')

    expect(mockedDiscardQuestionDraft).not.toHaveBeenCalled()
    expect(wrapper.text()).not.toContain('¿Descartar este borrador?')
  })

  it('discards draft and returns to the source question', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost())
    mockedDiscardQuestionDraft.mockResolvedValue()

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Descartar borrador').trigger('click')
    await lastButtonByText(wrapper, 'Descartar borrador').trigger('click')
    await flushPromises()

    expect(mockedDiscardQuestionDraft).toHaveBeenCalledWith(1)
    expect(routerPush).toHaveBeenCalledWith({
      name: 'admin-question-detail',
      params: { id: 1 },
    })
  })

  it('renders friendly discard error without technical details', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost())
    mockedDiscardQuestionDraft.mockRejectedValue(new Error('SQL detail'))

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Descartar borrador').trigger('click')
    await lastButtonByText(wrapper, 'Descartar borrador').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos descartar el borrador.')
    expect(wrapper.text()).not.toContain('SQL detail')
  })

  it('renders error state for missing drafts', async () => {
    mockedGetAdminPost.mockRejectedValue(new Error('Not found'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar este borrador.')
  })

  it('signs out and redirects when admin authorization fails', async () => {
    mockedGetAdminPost.mockRejectedValue(new Error('Unauthorized'))
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
  return mount(AdminPostDraftView, {
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

function adminPost(): AdminPost {
  return {
    id: 9,
    title: '',
    content: '',
    status: 'DRAFT',
    sourceQuestionId: 1,
    section: {
      id: 1,
      type: 'TALLER',
      name: 'Taller 1',
      slug: 'taller-1',
      description: null,
    },
    sourceQuestion: {
      id: 1,
      nickname: null,
      question: '<strong>No HTML</strong>',
      status: 'PENDING',
      createdAt: '2026-01-01T00:00:00Z',
      hasAttachment: true,
    },
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  }
}
