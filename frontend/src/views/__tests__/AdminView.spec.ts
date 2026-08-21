import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { signOut } from '@/services/firebase/authService'
import AdminView from '@/views/admin/AdminView.vue'

const routerPush = vi.hoisted(() => vi.fn<(route: unknown) => void>())

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedSignOut = vi.mocked(signOut)

describe('AdminView', () => {
  beforeEach(() => {
    routerPush.mockReset()
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders a minimal verified admin panel', () => {
    const wrapper = mount(AdminView)

    expect(wrapper.text()).toContain('Panel administrativo')
    expect(wrapper.text()).toContain('Sesión verificada')
  })

  it('signs out and redirects to login', async () => {
    const wrapper = mount(AdminView)

    await wrapper.get('button').trigger('click')

    expect(mockedSignOut).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith({ name: 'admin-login' })
  })
})
