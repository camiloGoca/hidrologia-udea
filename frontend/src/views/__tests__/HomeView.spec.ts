import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import HomeView from '@/views/HomeView.vue'

describe('HomeView', () => {
  it('renders the public home hero and main access cards', () => {
    const wrapper = mount(HomeView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a><slot /></a>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('Hidrología UdeA')
    expect(wrapper.text()).toContain('Conocimiento que fluye, ciencia que transforma.')
    expect(wrapper.text()).toContain('Enlaces de interés')
    expect(wrapper.text()).toContain('Talleres')
    expect(wrapper.text()).toContain('Parciales')
    expect(wrapper.text()).toContain('¿No encontraste tu duda?')
    expect(wrapper.text()).toContain('Agregar una pregunta')
  })
})
