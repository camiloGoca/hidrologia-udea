import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'

import AppHeader from '@/components/AppHeader.vue'

describe('AppHeader', () => {
  it('navigates to search with the submitted query', async () => {
    const router = createTestRouter()
    await router.push('/')
    await router.isReady()
    const wrapper = mount(AppHeader, { global: { plugins: [router] } })

    await wrapper.get('#site-search').setValue(' balance ')
    await wrapper.get('form[role="search"]').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('search')
    expect(router.currentRoute.value.query.q).toBe('balance')
  })

  it('keeps the search input synchronized with the route query', async () => {
    const router = createTestRouter()
    await router.push('/buscar?q=cuenca')
    await router.isReady()
    const wrapper = mount(AppHeader, { global: { plugins: [router] } })

    expect((wrapper.get('#site-search').element as HTMLInputElement).value).toBe('cuenca')

    await router.push('/buscar?q=balance')
    await router.isReady()

    expect((wrapper.get('#site-search').element as HTMLInputElement).value).toBe('balance')
  })
})

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/buscar', name: 'search', component: { template: '<div />' } },
      { path: '/enlaces', name: 'links', component: { template: '<div />' } },
      { path: '/talleres', name: 'workshops', component: { template: '<div />' } },
      { path: '/parciales', name: 'exams', component: { template: '<div />' } },
      { path: '/preguntas/nueva', name: 'new-question', component: { template: '<div />' } },
    ],
  })
}
