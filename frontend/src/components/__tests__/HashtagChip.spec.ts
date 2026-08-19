import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import HashtagChip from '@/components/HashtagChip.vue'

const routerLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a><slot /></a>',
}

describe('HashtagChip', () => {
  it('links to the public hashtag route', () => {
    const wrapper = mount(HashtagChip, {
      props: {
        tag: {
          name: 'Cuencas',
          slug: 'cuencas',
        },
      },
      global: {
        stubs: {
          RouterLink: routerLinkStub,
        },
      },
    })

    expect(wrapper.text()).toContain('#Cuencas')
    expect(wrapper.getComponent(routerLinkStub).props('to')).toEqual({
      name: 'hashtag-detail',
      params: { slug: 'cuencas' },
    })
  })
})
