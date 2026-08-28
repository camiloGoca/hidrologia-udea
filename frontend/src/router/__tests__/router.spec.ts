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
      routes.some((route) => route.path === '/publicaciones/:id' && route.name === 'post-detail'),
    ).toBe(true)
    expect(routes.some((route) => route.path === '/buscar' && route.name === 'search')).toBe(true)
    expect(
      routes.some((route) => route.path === '/hashtags/:slug' && route.name === 'hashtag-detail'),
    ).toBe(true)
    expect(
      routes.some((route) => route.path === '/preguntas/nueva' && route.name === 'new-question'),
    ).toBe(true)
    expect(routes.some((route) => route.path === '/admin' && route.name === 'admin-home')).toBe(
      true,
    )
    expect(
      routes.some((route) => route.path === '/admin/preguntas' && route.name === 'admin-questions'),
    ).toBe(true)
    expect(
      routes.some(
        (route) =>
          route.path === '/admin/preguntas/:id' && route.name === 'admin-question-detail',
      ),
    ).toBe(true)
    expect(
      routes.some(
        (route) =>
          route.path === '/admin/publicaciones/:id' && route.name === 'admin-post-detail',
      ),
    ).toBe(true)
    expect(
      routes.some((route) => route.path === '/admin/hashtags' && route.name === 'admin-hashtags'),
    ).toBe(true)
    expect(
      routes.some((route) => route.path === '/admin/enlaces' && route.name === 'admin-links'),
    ).toBe(true)
    expect(
      routes.some(
        (route) => route.path === '/admin/estadisticas' && route.name === 'admin-analytics',
      ),
    ).toBe(true)
    expect(
      routes.some((route) => route.path === '/:pathMatch(.*)*' && route.name === 'not-found'),
    ).toBe(true)
  })

  it('protects administrative routes with the admin guard', () => {
    const adminRoute = router.resolve('/admin/preguntas')
    const detailRoute = router.resolve('/admin/preguntas/1')
    const postRoute = router.resolve('/admin/publicaciones/9')
    const tagsRoute = router.resolve('/admin/hashtags')
    const linksRoute = router.resolve('/admin/enlaces')
    const analyticsRoute = router.resolve('/admin/estadisticas')

    expect(adminRoute.matched.some((record) => Boolean(record.beforeEnter))).toBe(true)
    expect(detailRoute.matched.some((record) => Boolean(record.beforeEnter))).toBe(true)
    expect(postRoute.matched.some((record) => Boolean(record.beforeEnter))).toBe(true)
    expect(tagsRoute.matched.some((record) => Boolean(record.beforeEnter))).toBe(true)
    expect(linksRoute.matched.some((record) => Boolean(record.beforeEnter))).toBe(true)
    expect(analyticsRoute.matched.some((record) => Boolean(record.beforeEnter))).toBe(true)
  })
})
