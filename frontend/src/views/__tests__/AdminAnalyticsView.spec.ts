import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { getAdminAnalyticsSummary } from '@/services/api/adminAnalyticsService'
import { signOut } from '@/services/firebase/authService'
import type { AdminAnalyticsSummary } from '@/types/analytics'
import AdminAnalyticsView from '@/views/admin/AdminAnalyticsView.vue'

const routerPush = vi.hoisted(() => vi.fn<(route: unknown) => void>())

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRouter: () => ({ push: routerPush }),
  }
})

vi.mock('@/services/api/adminAnalyticsService', () => ({
  getAdminAnalyticsSummary: vi.fn<() => Promise<AdminAnalyticsSummary>>(),
}))

vi.mock('@/services/api/adminErrors', () => ({
  isAdminAuthorizationError: vi.fn<(error: unknown) => boolean>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetSummary = vi.mocked(getAdminAnalyticsSummary)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminAnalyticsView', () => {
  beforeEach(() => {
    routerPush.mockReset()
    mockedGetSummary.mockReset()
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders loading state', () => {
    mockedGetSummary.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando estadísticas...')
  })

  it('renders summary metrics, rankings and question counts', async () => {
    mockedGetSummary.mockResolvedValue(summary())

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Resumen del sitio')
    expect(wrapper.text()).toContain('Total')
    expect(wrapper.text()).toContain('120')
    expect(wrapper.text()).toContain('Taller 1')
    expect(wrapper.text()).toContain('Publicación más leída')
    expect(wrapper.text()).toContain('Pendientes')
    expect(wrapper.text()).toContain('Publicadas/respondidas')
  })

  it('renders empty ranking states', async () => {
    mockedGetSummary.mockResolvedValue({
      ...summary(),
      mostViewedSections: [],
      mostViewedWorkshop: null,
      mostViewedExam: null,
      mostViewedPosts: [],
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Sin consultas de secciones todavía.')
    expect(wrapper.text()).toContain('Sin consultas de publicaciones todavía.')
    expect(wrapper.text()).toContain('Sin datos todavía')
  })

  it('renders a friendly error for non-auth failures', async () => {
    mockedGetSummary.mockRejectedValue(new Error('Network'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar las estadísticas.')
  })

  it('signs out and redirects on admin authorization errors', async () => {
    mockedGetSummary.mockRejectedValue(new Error('Forbidden'))
    mockedIsAdminAuthorizationError.mockReturnValue(true)

    mountView()
    await flushPromises()

    expect(mockedSignOut).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith({
      name: 'admin-login',
      query: { reason: 'forbidden' },
    })
  })
})

function mountView() {
  return mount(AdminAnalyticsView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })
}

function summary(): AdminAnalyticsSummary {
  return {
    totalVisits: 120,
    visitsToday: 4,
    visitsThisWeek: 18,
    visitsThisMonth: 51,
    mostViewedSections: [
      {
        id: 1,
        type: 'TALLER',
        name: 'Taller 1',
        slug: 'taller-1',
        views: 9,
      },
    ],
    mostViewedWorkshop: {
      id: 1,
      type: 'TALLER',
      name: 'Taller 1',
      slug: 'taller-1',
      views: 9,
    },
    mostViewedExam: {
      id: 4,
      type: 'PARCIAL',
      name: 'Parcial 1',
      slug: 'parcial-1',
      views: 5,
    },
    mostViewedPosts: [
      {
        id: 10,
        title: 'Publicación más leída',
        section: {
          id: 1,
          type: 'TALLER',
          name: 'Taller 1',
          slug: 'taller-1',
          description: null,
        },
        views: 7,
      },
    ],
    questions: {
      total: 8,
      pending: 2,
      published: 3,
    },
    dailyVisits: [
      { date: '2026-08-26', visits: 2 },
      { date: '2026-08-27', visits: 4 },
    ],
  }
}
