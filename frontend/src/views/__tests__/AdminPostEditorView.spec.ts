import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import {
  discardQuestionDraft,
  getAdminPost,
  publishAdminPost,
  updateAdminPostDraft,
} from '@/services/api/adminService'
import { getSections } from '@/services/api/sectionService'
import { signOut } from '@/services/firebase/authService'
import type { AdminPost } from '@/types/adminPost'
import type { Section } from '@/types/section'
import AdminPostEditorView from '@/views/admin/AdminPostEditorView.vue'

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
  updateAdminPostDraft: vi.fn<(id: number, payload: unknown) => Promise<AdminPost>>(),
  publishAdminPost: vi.fn<(id: number) => Promise<AdminPost>>(),
  discardQuestionDraft: vi.fn<(questionId: number) => Promise<void>>(),
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

const mockedGetAdminPost = vi.mocked(getAdminPost)
const mockedUpdateAdminPostDraft = vi.mocked(updateAdminPostDraft)
const mockedPublishAdminPost = vi.mocked(publishAdminPost)
const mockedDiscardQuestionDraft = vi.mocked(discardQuestionDraft)
const mockedGetSections = vi.mocked(getSections)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminPostEditorView', () => {
  beforeEach(() => {
    routeParams.id = '9'
    routerPush.mockReset()
    mockedGetAdminPost.mockReset()
    mockedUpdateAdminPostDraft.mockReset()
    mockedPublishAdminPost.mockReset()
    mockedDiscardQuestionDraft.mockReset()
    mockedGetSections.mockReset()
    mockedGetSections.mockResolvedValue(sections())
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders loading state while the draft is being fetched', () => {
    mockedGetAdminPost.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando publicación...')
  })

  it('renders draft editor with source question and no post image or hashtags controls', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost())

    const wrapper = mountView()
    await flushPromises()

    expect(mockedGetAdminPost).toHaveBeenCalledWith(9)
    expect(wrapper.text()).toContain('Editor de publicación')
    expect(wrapper.text()).toContain('BORRADOR')
    expect(wrapper.find('input').element.value).toBe('')
    expect(wrapper.find('textarea').element.value).toBe('')
    expect(wrapper.find('select').element.value).toBe('taller-1')
    expect(wrapper.text()).toContain('Anónimo')
    expect(wrapper.text()).toContain('Pregunta de origen')
    expect(wrapper.text()).toContain('La pregunta original tiene una imagen adjunta privada.')
    expect(wrapper.text()).toContain('<strong>No HTML</strong>')
    expect(wrapper.find('strong').exists()).toBe(false)
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Hashtags')
  })

  it('tracks dirty state and saves a draft manually', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost())
    mockedUpdateAdminPostDraft.mockResolvedValue(
      adminPost({
        title: 'Título guardado',
        content: 'Línea 1\nLínea 2',
        section: sections()[1],
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('input').setValue('Título guardado')
    await wrapper.find('textarea').setValue('Línea 1\nLínea 2')
    await wrapper.find('select').setValue('parcial-1')

    expect(wrapper.text()).toContain('Cambios sin guardar')
    expect(buttonByText(wrapper, 'Publicar').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Guarda los cambios antes de publicar.')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedUpdateAdminPostDraft).toHaveBeenCalledWith(9, {
      title: 'Título guardado',
      content: 'Línea 1\nLínea 2',
      sectionSlug: 'parcial-1',
    })
    expect(wrapper.text()).toContain('Borrador guardado.')
    expect(wrapper.text()).not.toContain('Cambios sin guardar')
  })

  it('renders save error without technical details', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost())
    mockedUpdateAdminPostDraft.mockRejectedValue(new Error('SQL detail'))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('input').setValue('Título')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos guardar el borrador.')
    expect(wrapper.text()).not.toContain('SQL detail')
  })

  it('keeps publish disabled when saved title is blank', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost({ title: '', content: '' }))

    const wrapper = mountView()
    await flushPromises()

    expect(buttonByText(wrapper, 'Publicar').attributes('disabled')).toBeDefined()
  })

  it('keeps publish disabled when saved content is blank', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost({ title: 'Título guardado', content: '' }))

    const wrapper = mountView()
    await flushPromises()

    expect(buttonByText(wrapper, 'Publicar').attributes('disabled')).toBeDefined()
  })

  it('keeps publish disabled while valid changes are dirty', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost({ title: '', content: '' }))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('input').setValue('Título')
    await wrapper.find('textarea').setValue('Contenido')

    expect(buttonByText(wrapper, 'Publicar').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Guarda los cambios antes de publicar.')
  })

  it('enables publish when valid title and content are already saved', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost({ title: 'Título', content: 'Contenido' }))

    const wrapper = mountView()
    await flushPromises()

    expect(buttonByText(wrapper, 'Publicar').attributes('disabled')).toBeUndefined()
  })

  it('opens publish modal, cancels, then publishes without double submit', async () => {
    const published = adminPost({
      title: 'Título',
      content: 'Contenido',
      status: 'PUBLISHED',
      sourceQuestion: {
        ...adminPost().sourceQuestion!,
        status: 'PUBLISHED',
      },
      publishedAt: '2026-01-02T00:00:00Z',
    })
    mockedGetAdminPost.mockResolvedValue(adminPost({ title: 'Título', content: 'Contenido' }))
    mockedPublishAdminPost.mockResolvedValue(published)

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Publicar').trigger('click')
    expect(wrapper.text()).toContain('¿Publicar esta publicación?')
    await buttonByText(wrapper, 'Cancelar').trigger('click')
    await flushPromises()
    expect(mockedPublishAdminPost).not.toHaveBeenCalled()

    await buttonByText(wrapper, 'Publicar').trigger('click')
    const confirmButton = lastButtonByText(wrapper, 'Publicar')
    await confirmButton.trigger('click')
    await confirmButton.trigger('click')
    await flushPromises()

    expect(mockedPublishAdminPost).toHaveBeenCalledTimes(1)
    expect(mockedPublishAdminPost).toHaveBeenCalledWith(9)
    expect(wrapper.text()).toContain('PUBLICADA')
    expect(wrapper.text()).toContain('Ver publicación pública')
    expect(wrapper.find('input').exists()).toBe(false)
    expect(wrapper.find('textarea').exists()).toBe(false)
  })

  it('keeps draft editable when publish fails', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost({ title: 'Título', content: 'Contenido' }))
    mockedPublishAdminPost.mockRejectedValue(new Error('Bad request'))

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Publicar').trigger('click')
    await lastButtonByText(wrapper, 'Publicar').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos publicar el borrador.')
    expect(wrapper.text()).toContain('BORRADOR')
    expect(wrapper.find('textarea').exists()).toBe(true)
    expect(wrapper.text()).toContain('Pregunta de origen')
  })

  it('renders published post as read-only with public and question links', async () => {
    mockedGetAdminPost.mockResolvedValue(
      adminPost({
        title: 'Título publicado',
        content: 'Contenido\nmultilínea',
        status: 'PUBLISHED',
        publishedAt: '2026-01-02T00:00:00Z',
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Publicación publicada')
    expect(wrapper.text()).toContain('PUBLICADA')
    expect(wrapper.text()).toContain('Contenido\nmultilínea')
    expect(wrapper.text()).toContain('Ver publicación pública')
    expect(wrapper.text()).toContain('Ver pregunta original')
    expect(wrapper.find('input').exists()).toBe(false)
    expect(wrapper.find('textarea').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Descartar borrador')
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

    expect(wrapper.text()).toContain('No pudimos cargar esta publicación.')
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
  return mount(AdminPostEditorView, {
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

function adminPost(overrides: Partial<AdminPost> = {}): AdminPost {
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
