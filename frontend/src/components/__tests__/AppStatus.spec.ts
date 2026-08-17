import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import AppStatus from '@/components/AppStatus.vue'

describe('AppStatus', () => {
  it('renders the base application status', () => {
    const wrapper = mount(AppStatus)

    expect(wrapper.text()).toContain('Aplicacion funcionando')
  })
})
