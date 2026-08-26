import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import {
  archiveAdminPost,
  discardManualAdminPost,
  getAdminPost,
  publishAdminPost,
  restoreAdminPost,
  updateAdminPost,
} from '@/services/api/adminPostService'
import { getAdminTags } from '@/services/api/adminTagService'
import { discardQuestionDraft } from '@/services/api/adminService'
import { getSections } from '@/services/api/sectionService'
import { signOut } from '@/services/firebase/authService'
import type { AdminTag } from '@/types/adminTag'
import type { AdminPost } from '@/types/adminPost'
import type { PostContentDocument } from '@/types/postContent'
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

vi.mock('@/services/api/adminPostService', () => ({
  getAdminPost: vi.fn<(id: number) => Promise<AdminPost>>(),
  updateAdminPost: vi.fn<(id: number, payload: unknown) => Promise<AdminPost>>(),
  publishAdminPost: vi.fn<(id: number) => Promise<AdminPost>>(),
  archiveAdminPost: vi.fn<(id: number) => Promise<AdminPost>>(),
  restoreAdminPost: vi.fn<(id: number) => Promise<AdminPost>>(),
  discardManualAdminPost: vi.fn<(id: number) => Promise<void>>(),
}))

vi.mock('@/services/api/adminTagService', () => ({
  getAdminTags: vi.fn<() => Promise<AdminTag[]>>(),
}))

vi.mock('@/services/api/adminService', () => ({
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
const mockedUpdateAdminPost = vi.mocked(updateAdminPost)
const mockedPublishAdminPost = vi.mocked(publishAdminPost)
const mockedArchiveAdminPost = vi.mocked(archiveAdminPost)
const mockedRestoreAdminPost = vi.mocked(restoreAdminPost)
const mockedDiscardManualAdminPost = vi.mocked(discardManualAdminPost)
const mockedGetAdminTags = vi.mocked(getAdminTags)
const mockedDiscardQuestionDraft = vi.mocked(discardQuestionDraft)
const mockedGetSections = vi.mocked(getSections)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminPostEditorView', () => {
  beforeEach(() => {
    routeParams.id = '9'
    routerPush.mockReset()
    mockedGetAdminPost.mockReset()
    mockedUpdateAdminPost.mockReset()
    mockedPublishAdminPost.mockReset()
    mockedArchiveAdminPost.mockReset()
    mockedRestoreAdminPost.mockReset()
    mockedDiscardManualAdminPost.mockReset()
    mockedGetAdminTags.mockReset()
    mockedGetAdminTags.mockResolvedValue(tags())
    mockedDiscardQuestionDraft.mockReset()
    mockedGetSections.mockReset()
    mockedGetSections.mockResolvedValue(sections())
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders loading state while the post is being fetched', () => {
    mockedGetAdminPost.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando publicación...')
  })

  it('renders draft editor with source question and hashtags controls but no post image', async () => {
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
    expect(wrapper.text()).toContain('Hashtags')
    expect(wrapper.text()).toContain('#Morfometría')
  })

  it('keeps the editor card from clipping the sticky toolbar', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost())

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('article').classes()).not.toContain('overflow-hidden')
  })

  it('tracks dirty state and saves a draft manually', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost())
    mockedUpdateAdminPost.mockResolvedValue(
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

    expect(mockedUpdateAdminPost).toHaveBeenCalledWith(9, {
      title: 'Título guardado',
      contentDocument: contentDocument('Línea 1\nLínea 2'),
      sectionSlug: 'parcial-1',
      tagIds: [],
    })
    expect(wrapper.text()).toContain('Borrador guardado.')
    expect(wrapper.text()).not.toContain('Cambios sin guardar')
  })

  it('renders assigned tags as selected and saves tagIds with the post', async () => {
    mockedGetAdminPost.mockResolvedValue(
      adminPost({
        title: 'Título',
        content: 'Contenido',
        tags: [tagAt(0)],
      }),
    )
    mockedUpdateAdminPost.mockResolvedValue(
      adminPost({
        title: 'Título',
        content: 'Contenido',
        tags: tags(),
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    const checkboxes = wrapper.findAll<HTMLInputElement>('input[type="checkbox"]')
    expect(checkboxes).toHaveLength(2)
    expect(checkboxes[0]!.element.checked).toBe(true)
    expect(checkboxes[1]!.element.checked).toBe(false)

    await checkboxes[1]!.setValue(true)
    expect(wrapper.text()).toContain('Cambios sin guardar')
    expect(buttonByText(wrapper, 'Publicar').attributes('disabled')).toBeDefined()

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedUpdateAdminPost).toHaveBeenCalledWith(9, {
      title: 'Título',
      contentDocument: contentDocument('Contenido'),
      sectionSlug: 'taller-1',
      tagIds: [1, 2],
    })
    expect(wrapper.text()).not.toContain('Cambios sin guardar')
  })

  it('blocks archive and restore while tag changes are dirty', async () => {
    mockedGetAdminPost.mockResolvedValueOnce(
      adminPost({
        title: 'Título',
        content: 'Contenido',
        status: 'PUBLISHED',
        publishedAt: '2026-01-02T00:00:00Z',
      }),
    )

    const publishedWrapper = mountView()
    await flushPromises()
    await publishedWrapper.find('input[type="checkbox"]').setValue(true)

    expect(buttonByText(publishedWrapper, 'Archivar publicación').attributes('disabled')).toBeDefined()

    mockedGetAdminPost.mockResolvedValueOnce(
      adminPost({
        title: 'Título',
        content: 'Contenido',
        status: 'ARCHIVED',
        publishedAt: '2026-01-02T00:00:00Z',
      }),
    )

    const archivedWrapper = mountView()
    await flushPromises()
    await archivedWrapper.find('input[type="checkbox"]').setValue(true)

    expect(buttonByText(archivedWrapper, 'Restaurar publicación').attributes('disabled')).toBeDefined()
  })

  it('keeps draft publish disabled until valid content is saved', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost({ title: '', content: '' }))

    const wrapper = mountView()
    await flushPromises()

    expect(buttonByText(wrapper, 'Publicar').attributes('disabled')).toBeDefined()

    await wrapper.find('input').setValue('Título')
    await wrapper.find('textarea').setValue('Contenido')

    expect(buttonByText(wrapper, 'Publicar').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Guarda los cambios antes de publicar.')
  })

  it('keeps publish disabled when saved title or content is blank', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost({ title: 'Título guardado', content: '' }))

    const wrapper = mountView()
    await flushPromises()

    expect(buttonByText(wrapper, 'Publicar').attributes('disabled')).toBeDefined()
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
  })

  it('edits a published post and keeps public actions blocked while dirty', async () => {
    mockedGetAdminPost.mockResolvedValue(
      adminPost({
        title: 'Título publicado',
        content: 'Contenido',
        status: 'PUBLISHED',
        publishedAt: '2026-01-02T00:00:00Z',
      }),
    )
    mockedUpdateAdminPost.mockResolvedValue(
      adminPost({
        title: 'Título actualizado',
        content: 'Contenido',
        status: 'PUBLISHED',
        publishedAt: '2026-01-02T00:00:00Z',
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Los cambios guardados se reflejarán inmediatamente')
    expect(wrapper.find('input').exists()).toBe(true)
    expect(wrapper.find('textarea').exists()).toBe(true)
    expect(wrapper.text()).toContain('Ver publicación pública')
    expect(wrapper.text()).not.toContain('Descartar borrador')

    await wrapper.find('input').setValue('Título actualizado')
    expect(buttonByText(wrapper, 'Archivar publicación').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Guarda los cambios antes de archivar.')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedUpdateAdminPost).toHaveBeenCalledWith(9, {
      title: 'Título actualizado',
      contentDocument: contentDocument('Contenido'),
      sectionSlug: 'taller-1',
      tagIds: [],
    })
    expect(wrapper.text()).toContain('Publicación actualizada.')
    expect(wrapper.text()).toContain('PUBLICADA')
  })

  it('keeps save disabled for published posts with blank content', async () => {
    mockedGetAdminPost.mockResolvedValue(
      adminPost({
        title: 'Título publicado',
        content: 'Contenido',
        status: 'PUBLISHED',
        publishedAt: '2026-01-02T00:00:00Z',
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('textarea').setValue('   ')

    expect(buttonByText(wrapper, 'Guardar cambios').attributes('disabled')).toBeDefined()
  })

  it('archives a published post through an accessible modal', async () => {
    mockedGetAdminPost.mockResolvedValue(
      adminPost({ title: 'Título', content: 'Contenido', status: 'PUBLISHED', publishedAt: '2026-01-02T00:00:00Z' }),
    )
    mockedArchiveAdminPost.mockResolvedValue(
      adminPost({ title: 'Título', content: 'Contenido', status: 'ARCHIVED', publishedAt: '2026-01-02T00:00:00Z' }),
    )

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Archivar publicación').trigger('click')
    expect(wrapper.text()).toContain('¿Archivar esta publicación?')
    await buttonByText(wrapper, 'Cancelar').trigger('click')
    expect(mockedArchiveAdminPost).not.toHaveBeenCalled()

    await buttonByText(wrapper, 'Archivar publicación').trigger('click')
    await lastButtonByText(wrapper, 'Archivar publicación').trigger('click')
    await flushPromises()

    expect(mockedArchiveAdminPost).toHaveBeenCalledWith(9)
    expect(wrapper.text()).toContain('ARCHIVADA')
    expect(wrapper.text()).toContain('Publicación archivada.')
    expect(wrapper.text()).not.toContain('Ver publicación pública')
  })

  it('edits and restores an archived post after saving changes', async () => {
    mockedGetAdminPost.mockResolvedValue(
      adminPost({ title: 'Título', content: 'Contenido', status: 'ARCHIVED', publishedAt: '2026-01-02T00:00:00Z' }),
    )
    mockedUpdateAdminPost.mockResolvedValue(
      adminPost({
        title: 'Título archivado',
        content: 'Contenido',
        status: 'ARCHIVED',
        publishedAt: '2026-01-02T00:00:00Z',
      }),
    )
    mockedRestoreAdminPost.mockResolvedValue(
      adminPost({
        title: 'Título archivado',
        content: 'Contenido',
        status: 'PUBLISHED',
        publishedAt: '2026-01-02T00:00:00Z',
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('ARCHIVADA')
    expect(wrapper.text()).not.toContain('Ver publicación pública')
    expect(wrapper.text()).not.toContain('Publicar')

    await wrapper.find('input').setValue('Título archivado')
    expect(buttonByText(wrapper, 'Restaurar publicación').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Guarda los cambios antes de restaurar.')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Publicación actualizada.')
    await buttonByText(wrapper, 'Restaurar publicación').trigger('click')
    expect(wrapper.text()).toContain('¿Restaurar esta publicación?')
    await lastButtonByText(wrapper, 'Restaurar publicación').trigger('click')
    await flushPromises()

    expect(mockedRestoreAdminPost).toHaveBeenCalledWith(9)
    expect(wrapper.text()).toContain('PUBLICADA')
    expect(wrapper.text()).toContain('Publicación restaurada.')
  })

  it('works without a source question reference', async () => {
    mockedGetAdminPost.mockResolvedValue(
      adminPost({
        title: 'Publicación directa',
        content: 'Contenido',
        status: 'PUBLISHED',
        sourceQuestionId: null,
        sourceQuestion: null,
        publishedAt: '2026-01-02T00:00:00Z',
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).not.toContain('Pregunta de origen')
    expect(wrapper.text()).not.toContain('Ver pregunta original')
  })

  it('discards a manual draft without calling the question draft endpoint', async () => {
    mockedGetAdminPost.mockResolvedValue(
      adminPost({
        sourceQuestionId: null,
        sourceQuestion: null,
      }),
    )
    mockedDiscardManualAdminPost.mockResolvedValue()

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Descartar borrador').trigger('click')
    expect(wrapper.text()).toContain('No hay una pregunta de origen asociada')
    await lastButtonByText(wrapper, 'Descartar borrador').trigger('click')
    await flushPromises()

    expect(mockedDiscardManualAdminPost).toHaveBeenCalledWith(9)
    expect(mockedDiscardQuestionDraft).not.toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith({
      name: 'admin-posts',
      query: { estado: 'borradores' },
    })
  })

  it('renders a friendly save error without technical details', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost({ title: 'Título', content: 'Contenido' }))
    mockedUpdateAdminPost.mockRejectedValue(new Error('SQL detail'))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('input').setValue('Título cambiado')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos guardar los cambios.')
    expect(wrapper.text()).not.toContain('SQL detail')
    expect(mockedSignOut).not.toHaveBeenCalled()
    expect(routerPush).not.toHaveBeenCalled()
  })

  it('renders a friendly action error without technical details', async () => {
    mockedGetAdminPost.mockResolvedValue(adminPost({ title: 'Título', content: 'Contenido' }))
    mockedPublishAdminPost.mockRejectedValue(new Error('Cloud detail'))

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Publicar').trigger('click')
    await lastButtonByText(wrapper, 'Publicar').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos completar la acción.')
    expect(wrapper.text()).not.toContain('Cloud detail')
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
    expect(mockedDiscardManualAdminPost).not.toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith({
      name: 'admin-question-detail',
      params: { id: 1 },
    })
  })

  it('renders error state for missing posts', async () => {
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
        AcademicPostEditor: {
          props: ['id', 'modelValue'],
          emits: ['update:modelValue'],
          computed: {
            text() {
              return extractText(this.modelValue as PostContentDocument)
            },
          },
          methods: {
            doc(value: string) {
              return contentDocument(value)
            },
          },
          template: '<textarea :id="id" :value="text" @input="$emit(\'update:modelValue\', doc($event.target.value))" />',
        },
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
  const content = overrides.content ?? ''

  return {
    id: 9,
    title: '',
    content,
    contentDocument: overrides.contentDocument ?? contentDocument(content),
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
    tags: [],
    ...overrides,
  }
}

function contentDocument(text: string): PostContentDocument {
  return {
    type: 'doc',
    content: text
      ? [
          {
            type: 'paragraph',
            content: [{ type: 'text', text }],
          },
        ]
      : [{ type: 'paragraph' }],
  }
}

function extractText(document: PostContentDocument): string {
  return (document.content ?? [])
    .flatMap((node) => node.content ?? [])
    .map((node) => node.text ?? '')
    .join('')
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

function tags(): AdminTag[] {
  return [
    {
      id: 1,
      name: 'Morfometría',
      slug: 'morfometria',
      usageCount: 1,
    },
    {
      id: 2,
      name: 'Cuencas',
      slug: 'cuencas',
      usageCount: 0,
    },
  ]
}

function tagAt(index: number): AdminTag {
  const tag = tags()[index]
  if (!tag) {
    throw new Error(`Missing tag at index ${index}`)
  }

  return tag
}
