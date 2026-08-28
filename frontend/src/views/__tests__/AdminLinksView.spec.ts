import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import {
  createAdminLink,
  deleteAdminLink,
  getAdminLinks,
  updateAdminLink,
} from '@/services/api/adminLinkService'
import { signOut } from '@/services/firebase/authService'
import type { AdminInterestingLink } from '@/types/interestingLink'
import AdminLinksView from '@/views/admin/AdminLinksView.vue'

const routerPush = vi.hoisted(() => vi.fn<(route: unknown) => void>())

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRouter: () => ({ push: routerPush }),
  }
})

vi.mock('@/services/api/adminLinkService', () => ({
  getAdminLinks: vi.fn<() => Promise<AdminInterestingLink[]>>(),
  createAdminLink: vi.fn<(payload: unknown) => Promise<AdminInterestingLink>>(),
  updateAdminLink: vi.fn<(linkId: number, payload: unknown) => Promise<AdminInterestingLink>>(),
  deleteAdminLink: vi.fn<(linkId: number) => Promise<void>>(),
}))

vi.mock('@/services/api/adminErrors', () => ({
  isAdminAuthorizationError: vi.fn<(error: unknown) => boolean>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetAdminLinks = vi.mocked(getAdminLinks)
const mockedCreateAdminLink = vi.mocked(createAdminLink)
const mockedUpdateAdminLink = vi.mocked(updateAdminLink)
const mockedDeleteAdminLink = vi.mocked(deleteAdminLink)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminLinksView', () => {
  beforeEach(() => {
    routerPush.mockReset()
    mockedGetAdminLinks.mockReset()
    mockedCreateAdminLink.mockReset()
    mockedUpdateAdminLink.mockReset()
    mockedDeleteAdminLink.mockReset()
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders loading state', () => {
    mockedGetAdminLinks.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando enlaces...')
  })

  it('renders error state', async () => {
    mockedGetAdminLinks.mockRejectedValue(new Error('Network'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar los enlaces.')
  })

  it('renders empty state', async () => {
    mockedGetAdminLinks.mockResolvedValue([])

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Aún no hay enlaces creados.')
  })

  it('renders links with active status and safe external action', async () => {
    mockedGetAdminLinks.mockResolvedValue([link(), link({ id: 2, title: 'Archivo', active: false })])

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('IDEAM')
    expect(wrapper.text()).toContain('Activo')
    expect(wrapper.text()).toContain('Archivo')
    expect(wrapper.text()).toContain('Inactivo')
    expect(wrapper.get('a[href="https://example.edu"]').attributes('rel')).toBe('noopener noreferrer')
  })

  it('creates a link and avoids double submit while submitting', async () => {
    mockedGetAdminLinks.mockResolvedValueOnce([])
    mockedGetAdminLinks.mockResolvedValueOnce([link()])
    let resolveCreate: (value: AdminInterestingLink) => void = () => undefined
    mockedCreateAdminLink.mockReturnValue(
      new Promise((resolve) => {
        resolveCreate = resolve
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('#new-link-title-input').setValue('  IDEAM  ')
    await wrapper.get('#new-link-url').setValue(' https://example.edu ')
    await wrapper.get('#new-link-description').setValue(' Recurso oficial ')
    await wrapper.get('#new-link-order').setValue(2)
    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')

    expect(buttonByText(wrapper, 'Creando...').attributes('disabled')).toBeDefined()
    expect(mockedCreateAdminLink).toHaveBeenCalledTimes(1)

    resolveCreate(link())
    await flushPromises()

    expect(mockedCreateAdminLink).toHaveBeenCalledWith({
      title: 'IDEAM',
      description: 'Recurso oficial',
      url: 'https://example.edu',
      displayOrder: 2,
      active: true,
    })
    expect(wrapper.text()).toContain('Enlace creado.')
    expect(wrapper.text()).toContain('IDEAM')
  })

  it('validates create form before calling the API', async () => {
    mockedGetAdminLinks.mockResolvedValue([])

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('#new-link-title-input').setValue('IDEAM')
    await wrapper.get('#new-link-url').setValue('ftp://example.edu')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('URL http o https válida')
    expect(mockedCreateAdminLink).not.toHaveBeenCalled()
  })

  it('edits a link and refreshes the list', async () => {
    mockedGetAdminLinks.mockResolvedValueOnce([link()])
    mockedGetAdminLinks.mockResolvedValueOnce([link({ title: 'IDEAM actualizado', active: false })])
    mockedUpdateAdminLink.mockResolvedValue(link({ title: 'IDEAM actualizado', active: false }))

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Editar').trigger('click')
    await wrapper.get('#edit-link-title-1').setValue('IDEAM actualizado')
    await wrapper.get('#edit-link-url-1').setValue('https://example.edu/nuevo')
    await wrapper.get('#edit-link-description-1').setValue('')
    await wrapper.get('#edit-link-order-1').setValue(3)
    await wrapper.findAll<HTMLInputElement>('input[type="checkbox"]')[1]!.setValue(false)
    await wrapper.findAll('form')[1]!.trigger('submit')
    await flushPromises()

    expect(mockedUpdateAdminLink).toHaveBeenCalledWith(1, {
      title: 'IDEAM actualizado',
      description: null,
      url: 'https://example.edu/nuevo',
      displayOrder: 3,
      active: false,
    })
    expect(wrapper.text()).toContain('IDEAM actualizado')
    expect(wrapper.text()).toContain('Inactivo')
  })

  it('shows a friendly edit validation error', async () => {
    mockedGetAdminLinks.mockResolvedValue([link()])

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Editar').trigger('click')
    await wrapper.get('#edit-link-url-1').setValue('nota')
    await wrapper.findAll('form')[1]!.trigger('submit')

    expect(wrapper.text()).toContain('URL http o https válida')
    expect(mockedUpdateAdminLink).not.toHaveBeenCalled()
  })

  it('opens delete modal, cancels it, then deletes the selected link', async () => {
    mockedGetAdminLinks.mockResolvedValueOnce([link(), link({ id: 2, title: 'Archivo' })])
    mockedGetAdminLinks.mockResolvedValueOnce([link({ id: 2, title: 'Archivo' })])
    mockedDeleteAdminLink.mockResolvedValue()

    const wrapper = mountView()
    await flushPromises()

    await buttonByText(wrapper, 'Eliminar').trigger('click')
    expect(wrapper.text()).toContain('¿Eliminar este enlace?')
    await buttonByText(wrapper, 'Cancelar').trigger('click')
    expect(mockedDeleteAdminLink).not.toHaveBeenCalled()

    await buttonByText(wrapper, 'Eliminar').trigger('click')
    await buttonByText(wrapper, 'Eliminar enlace').trigger('click')
    await flushPromises()

    expect(mockedDeleteAdminLink).toHaveBeenCalledWith(1)
    expect(wrapper.text()).not.toContain('IDEAM')
    expect(wrapper.text()).toContain('Archivo')
  })

  it('renders friendly create and delete errors', async () => {
    mockedGetAdminLinks.mockResolvedValue([link()])
    mockedCreateAdminLink.mockRejectedValue(new Error('SQL detail'))
    mockedDeleteAdminLink.mockRejectedValue(new Error('SQL detail'))

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('#new-link-title-input').setValue('IDEAM')
    await wrapper.get('#new-link-url').setValue('https://example.edu')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos crear el enlace.')
    expect(wrapper.text()).not.toContain('SQL detail')

    await buttonByText(wrapper, 'Eliminar').trigger('click')
    await buttonByText(wrapper, 'Eliminar enlace').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos eliminar el enlace.')
    expect(wrapper.text()).not.toContain('SQL detail')
  })

  it('signs out and redirects when admin authorization fails', async () => {
    mockedGetAdminLinks.mockRejectedValue(new Error('Unauthorized'))
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
  return mount(AdminLinksView)
}

function buttonByText(wrapper: ReturnType<typeof mountView>, text: string) {
  const button = wrapper.findAll('button').find((candidate) => candidate.text() === text)
  if (!button) {
    throw new Error(`Button not found: ${text}`)
  }

  return button
}

function link(overrides: Partial<AdminInterestingLink> = {}): AdminInterestingLink {
  return {
    id: 1,
    title: 'IDEAM',
    description: 'Recurso oficial',
    url: 'https://example.edu',
    displayOrder: 1,
    active: true,
    ...overrides,
  }
}
