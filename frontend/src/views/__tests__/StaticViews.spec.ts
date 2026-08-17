import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import LinksView from '@/views/static/LinksView.vue'
import NewQuestionView from '@/views/static/NewQuestionView.vue'
import NotFoundView from '@/views/static/NotFoundView.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>',
}

describe('static public views', () => {
  it('renders LinksView as a structural page', () => {
    const wrapper = mount(LinksView)

    expect(wrapper.text()).toContain('Enlaces de interés')
    expect(wrapper.text()).toContain('Los recursos recomendados por el profesor')
  })

  it('renders NewQuestionView as a structural page', () => {
    const wrapper = mount(NewQuestionView)

    expect(wrapper.text()).toContain('Agregar una pregunta')
    expect(wrapper.text()).toContain('permitirá enviar una pregunta')
  })

  it('renders NotFoundView', () => {
    const wrapper = mount(NotFoundView, {
      global: {
        stubs: { RouterLink: routerLinkStub },
      },
    })

    expect(wrapper.text()).toContain('Página no encontrada')
  })
})
