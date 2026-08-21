import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getAdminMe } from '@/services/api/adminService'
import { signIn, signOut } from '@/services/firebase/authService'
import AdminLoginView from '@/views/admin/AdminLoginView.vue'

const routerPush = vi.hoisted(() => vi.fn<(route: unknown) => void>())
const routeState = vi.hoisted(() => ({ query: {} as Record<string, string> }))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
  useRoute: () => routeState,
}))

vi.mock('@/services/firebase/authService', () => ({
  signIn: vi.fn<() => Promise<unknown>>(),
  signOut: vi.fn<() => Promise<void>>(),
}))

vi.mock('@/services/api/adminService', () => ({
  getAdminMe: vi.fn<() => Promise<{ authenticated: boolean; role: 'ADMIN' }>>(),
}))

const mockedSignIn = vi.mocked(signIn)
const mockedSignOut = vi.mocked(signOut)
const mockedGetAdminMe = vi.mocked(getAdminMe)

function mountView() {
  return mount(AdminLoginView)
}

describe('AdminLoginView', () => {
  beforeEach(() => {
    routeState.query = {}
    routerPush.mockReset()
    mockedSignIn.mockReset()
    mockedSignOut.mockReset()
    mockedGetAdminMe.mockReset()
    mockedSignOut.mockResolvedValue()
    mockedGetAdminMe.mockResolvedValue({ authenticated: true, role: 'ADMIN' })
  })

  it('signs in and validates the backend admin session', async () => {
    mockedSignIn.mockResolvedValue({ uid: 'admin-uid' } as Awaited<ReturnType<typeof signIn>>)
    const wrapper = mountView()

    await wrapper.get('input#admin-email').setValue('  profesor@example.com  ')
    await wrapper.get('input#admin-password').setValue('secret')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mockedSignIn).toHaveBeenCalledWith('profesor@example.com', 'secret')
    expect(mockedGetAdminMe).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith({ name: 'admin-home' })
  })

  it('shows a generic error when credentials fail', async () => {
    mockedSignIn.mockRejectedValue(new Error('Firebase error'))
    const wrapper = mountView()

    await wrapper.get('input#admin-email').setValue('profesor@example.com')
    await wrapper.get('input#admin-password').setValue('wrong-password')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('No fue posible iniciar sesión. Verifica tus credenciales.')
    expect(wrapper.text()).not.toContain('Crear cuenta')
    expect(wrapper.text()).not.toContain('Olvidé mi contraseña')
    expect(mockedSignOut).toHaveBeenCalled()
  })

  it('disables the button while submitting', async () => {
    mockedSignIn.mockReturnValue(new Promise(() => undefined))
    const wrapper = mountView()

    await wrapper.get('input#admin-email').setValue('profesor@example.com')
    await wrapper.get('input#admin-password').setValue('secret')
    await wrapper.get('form').trigger('submit')

    const button = wrapper.get('button[type="submit"]')
    expect(button.attributes('disabled')).toBeDefined()
    expect(button.text()).toContain('Iniciando sesión...')
  })

  it('shows unauthorized access message after a forbidden redirect', () => {
    routeState.query = { reason: 'forbidden' }

    const wrapper = mountView()

    expect(wrapper.text()).toContain('no está autorizada como administrador')
  })
})
