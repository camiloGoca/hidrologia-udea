import { RouterLinkStub, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { signOut } from '@/services/firebase/authService'
import AdminLayout from '@/layouts/AdminLayout.vue'

const routerPush = vi.hoisted(() => vi.fn<(route: unknown) => void>())

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRouter: () => ({ push: routerPush }),
  }
})

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedSignOut = vi.mocked(signOut)

describe('AdminLayout', () => {
  beforeEach(() => {
    routerPush.mockReset()
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders the administrative navigation', () => {
    const wrapper = mount(AdminLayout, {
      global: {
        stubs: {
          RouterLink: RouterLinkStub,
          RouterView: { template: '<div />' },
        },
      },
    })

    expect(wrapper.text()).toContain('Hidrología UdeA')
    expect(wrapper.text()).toContain('Inicio')
    expect(wrapper.text()).toContain('Preguntas pendientes')
    expect(wrapper.text()).toContain('Cerrar sesión')
  })

  it('signs out and redirects to login', async () => {
    const wrapper = mount(AdminLayout, {
      global: {
        stubs: {
          RouterLink: RouterLinkStub,
          RouterView: { template: '<div />' },
        },
      },
    })

    await wrapper.get('button').trigger('click')

    expect(mockedSignOut).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith({ name: 'admin-login' })
  })
})
