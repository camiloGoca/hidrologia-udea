import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import SectionCard from '@/components/SectionCard.vue'
import type { Section } from '@/types/section'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>',
}

function section(type: Section['type'], name: string): Section {
  return {
    id: type === 'TALLER' ? 1 : 4,
    type,
    name,
    slug: type === 'TALLER' ? 'taller-1' : 'parcial-1',
    description: null,
    displayOrder: type === 'TALLER' ? 1 : 4,
  }
}

function mountCard(section: Section, position = 1) {
  return mount(SectionCard, {
    props: {
      section,
      position,
      to: '/',
    },
    global: {
      stubs: { RouterLink: routerLinkStub },
    },
  })
}

describe('SectionCard', () => {
  it('reuses the same illustration for matching taller and parcial positions', () => {
    const tallerCard = mountCard(section('TALLER', 'Taller 1'), 1)
    const parcialCard = mountCard(section('PARCIAL', 'Parcial 1'), 1)

    expect(parcialCard.find('svg').html()).toBe(tallerCard.find('svg').html())
  })

  it('shows the current parcial fallback copy when there is no description', () => {
    const wrapper = mountCard(section('PARCIAL', 'Parcial 1'))

    expect(wrapper.text()).toContain('Consulta las publicaciones asociadas a este parcial.')
    expect(wrapper.text()).not.toContain('Las publicaciones asociadas se mostrarán')
  })
})
