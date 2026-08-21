import { RouterLinkStub, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AdminView from '@/views/admin/AdminView.vue'

describe('AdminView', () => {
  it('renders a minimal verified admin panel with a questions access', () => {
    const wrapper = mount(AdminView, {
      global: {
        stubs: {
          RouterLink: RouterLinkStub,
        },
      },
    })

    expect(wrapper.text()).toContain('Panel administrativo')
    expect(wrapper.text()).toContain('Sesión verificada')
    expect(wrapper.text()).toContain('Ver preguntas')
    expect(wrapper.getComponent(RouterLinkStub).props('to')).toEqual({ name: 'admin-questions' })
  })
})
