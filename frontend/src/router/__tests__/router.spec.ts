import { describe, expect, it } from 'vitest'

import router from '@/router'

describe('router', () => {
  it('registers the main public routes', () => {
    const routes = router.getRoutes()

    expect(routes.some((route) => route.path === '/' && route.name === 'home')).toBe(true)
    expect(routes.some((route) => route.path === '/talleres' && route.name === 'workshops')).toBe(
      true,
    )
    expect(routes.some((route) => route.path === '/parciales' && route.name === 'exams')).toBe(
      true,
    )
    expect(routes.some((route) => route.path === '/enlaces' && route.name === 'links')).toBe(true)
    expect(
      routes.some((route) => route.path === '/preguntas/nueva' && route.name === 'new-question'),
    ).toBe(true)
    expect(
      routes.some((route) => route.path === '/:pathMatch(.*)*' && route.name === 'not-found'),
    ).toBe(true)
  })
})
