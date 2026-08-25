import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import {
  createAdminTag,
  deleteAdminTag,
  getAdminTags,
  renameAdminTag,
} from '@/services/api/adminTagService'
import { signOut } from '@/services/firebase/authService'
import type { AdminTag } from '@/types/adminTag'
import AdminHashtagsView from '@/views/admin/AdminHashtagsView.vue'

const routerPush = vi.hoisted(() => vi.fn<(route: unknown) => void>())

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRouter: () => ({ push: routerPush }),
  }
})

vi.mock('@/services/api/adminTagService', () => ({
  getAdminTags: vi.fn<() => Promise<AdminTag[]>>(),
  createAdminTag: vi.fn<(payload: { name: string }) => Promise<AdminTag>>(),
  renameAdminTag: vi.fn<(tagId: number, payload: { name: string }) => Promise<AdminTag>>(),
  deleteAdminTag: vi.fn<(tagId: number) => Promise<void>>(),
}))

vi.mock('@/services/api/adminErrors', () => ({
  isAdminAuthorizationError: vi.fn<(error: unknown) => boolean>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetAdminTags = vi.mocked(getAdminTags)
const mockedCreateAdminTag = vi.mocked(createAdminTag)
const mockedRenameAdminTag = vi.mocked(renameAdminTag)
const mockedDeleteAdminTag = vi.mocked(deleteAdminTag)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminHashtagsView', () => {
  beforeEach(() => {
    routerPush.mockReset()
    mockedGetAdminTags.mockReset()
    mockedCreateAdminTag.mockReset()
    mockedRenameAdminTag.mockReset()
    mockedDeleteAdminTag.mockReset()
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders loading state', () => {
    mockedGetAdminTags.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando hashtags...')
  })

  it('renders error state', async () => {
    mockedGetAdminTags.mockRejectedValue(new Error('Network'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar los hashtags.')
  })

  it('renders empty state', async () => {
    mockedGetAdminTags.mockResolvedValue([])

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Aún no hay hashtags creados.')
  })

  it('renders tags with usage count and public links', async () => {
    mockedGetAdminTags.mockResolvedValue(tags())

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('#Morfometría')
    expect(wrapper.text()).toContain('morfometria')
    expect(wrapper.text()).toContain('Usado en 3 publicaciones')
    expect(wrapper.text()).toContain('#Cuencas')
    expect(wrapper.text()).toContain('Sin uso')
    expect(wrapper.findAllComponents(RouterLinkStub)[0]!.props('to')).toEqual({
      name: 'hashtag-detail',
      params: { slug: 'morfometria' },
    })
  })

  it('creates a tag and avoids double submit while submitting', async () => {
    mockedGetAdminTags.mockResolvedValueOnce([])
    mockedGetAdminTags.mockResolvedValueOnce([tagAt(0)])
    let resolveCreate: (tag: AdminTag) => void = () => undefined
    mockedCreateAdminTag.mockReturnValue(
      new Promise((resolve) => {
        resolveCreate = resolve
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('input').setValue('Morfometría')
    await wrapper.find('form').trigger('submit')
    await wrapper.find('form').trigger('submit')

    expect(buttonByText(wrapper, 'Creando...').attributes('disabled')).toBeDefined()
    expect(mockedCreateAdminTag).toHaveBeenCalledTimes(1)

    resolveCreate(tagAt(0))
    await flushPromises()

    expect(mockedCreateAdminTag).toHaveBeenCalledWith({ name: 'Morfometría' })
    expect(wrapper.text()).toContain('Hashtag creado.')
    expect(wrapper.text()).toContain('#Morfometría')
  })

  it('renders friendly duplicate create error', async () => {
    mockedGetAdminTags.mockResolvedValue([])
    mockedCreateAdminTag.mockRejectedValue(new Error('constraint detail'))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('input').setValue('Morfometría')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos crear el hashtag.')
    expect(wrapper.text()).not.toContain('constraint detail')
  })

  it('opens and cancels rename modal', async () => {
    mockedGetAdminTags.mockResolvedValue(tags())

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Renombrar').trigger('click')
    expect(wrapper.text()).toContain('La URL del hashtag se conservará.')
    await buttonByText(wrapper, 'Cancelar').trigger('click')

    expect(mockedRenameAdminTag).not.toHaveBeenCalled()
  })

  it('renames a tag and keeps the slug visible', async () => {
    mockedGetAdminTags.mockResolvedValueOnce(tags())
    mockedGetAdminTags.mockResolvedValueOnce([
      { ...tagAt(0), name: 'Morfometría de cuencas' },
      tagAt(1),
    ])
    mockedRenameAdminTag.mockResolvedValue({ ...tagAt(0), name: 'Morfometría de cuencas' })

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Renombrar').trigger('click')
    await wrapper.findAll('input')[1]!.setValue('Morfometría de cuencas')
    await buttonByText(wrapper, 'Guardar nombre').trigger('click')
    await flushPromises()

    expect(mockedRenameAdminTag).toHaveBeenCalledWith(1, { name: 'Morfometría de cuencas' })
    expect(wrapper.text()).toContain('#Morfometría de cuencas')
    expect(wrapper.text()).toContain('morfometria')
  })

  it('opens delete modal for unused tags and cancels it', async () => {
    mockedGetAdminTags.mockResolvedValue(tags())

    const wrapper = mountView()
    await flushPromises()

    await buttonsByText(wrapper, 'Eliminar')[1]!.trigger('click')
    expect(wrapper.text()).toContain('¿Eliminar este hashtag?')
    await buttonByText(wrapper, 'Cancelar').trigger('click')

    expect(mockedDeleteAdminTag).not.toHaveBeenCalled()
  })

  it('deletes unused tags', async () => {
    mockedGetAdminTags.mockResolvedValueOnce(tags())
    mockedGetAdminTags.mockResolvedValueOnce([tagAt(0)])
    mockedDeleteAdminTag.mockResolvedValue()

    const wrapper = mountView()
    await flushPromises()

    await buttonsByText(wrapper, 'Eliminar')[1]!.trigger('click')
    await buttonByText(wrapper, 'Eliminar hashtag').trigger('click')
    await flushPromises()

    expect(mockedDeleteAdminTag).toHaveBeenCalledWith(2)
    expect(wrapper.text()).not.toContain('#Cuencas')
  })

  it('does not allow deleting used tags', async () => {
    mockedGetAdminTags.mockResolvedValue(tags())

    const wrapper = mountView()
    await flushPromises()

    expect(buttonsByText(wrapper, 'Eliminar')[0]!.attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Quita este hashtag de las publicaciones antes de eliminarlo.')
  })

  it('signs out and redirects when admin authorization fails', async () => {
    mockedGetAdminTags.mockRejectedValue(new Error('Unauthorized'))
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
  return mount(AdminHashtagsView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })
}

function buttonByText(wrapper: ReturnType<typeof mountView>, text: string) {
  const button = buttonsByText(wrapper, text)[0]
  if (!button) {
    throw new Error(`Button not found: ${text}`)
  }

  return button
}

function buttonsByText(wrapper: ReturnType<typeof mountView>, text: string) {
  return wrapper.findAll('button').filter((candidate) => candidate.text() === text)
}

function tags(): AdminTag[] {
  return [
    {
      id: 1,
      name: 'Morfometría',
      slug: 'morfometria',
      usageCount: 3,
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
