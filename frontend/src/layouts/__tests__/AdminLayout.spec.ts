import { RouterLinkStub, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import AdminLayout from '@/layouts/AdminLayout.vue'
import { signOut } from '@/services/firebase/authService'

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
    expect(wrapper.text()).toContain('Preguntas')
    expect(wrapper.text()).toContain('Publicaciones')
    expect(wrapper.text()).toContain('Hashtags')
    expect(wrapper.text()).toContain('Enlaces')
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
