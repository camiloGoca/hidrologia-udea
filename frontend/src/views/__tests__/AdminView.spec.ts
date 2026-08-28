import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { getAdminAnalyticsSummary } from '@/services/api/adminAnalyticsService'
import { getPostsByStatus } from '@/services/api/adminPostService'
import { signOut } from '@/services/firebase/authService'
import type { AdminAnalyticsSummary } from '@/types/analytics'
import type { AdminPostsResponse } from '@/types/adminPost'
import AdminView from '@/views/admin/AdminView.vue'

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

vi.mock('@/services/api/adminPostService', () => ({
  getPostsByStatus: vi.fn<() => Promise<AdminPostsResponse>>(),
}))

vi.mock('@/services/api/adminErrors', () => ({
  isAdminAuthorizationError: vi.fn<(error: unknown) => boolean>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetSummary = vi.mocked(getAdminAnalyticsSummary)
const mockedGetPostsByStatus = vi.mocked(getPostsByStatus)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminView', () => {
  beforeEach(() => {
    routerPush.mockReset()
    mockedGetSummary.mockReset()
    mockedGetPostsByStatus.mockReset()
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders a loading state while the dashboard summary is requested', () => {
    mockedGetSummary.mockReturnValue(new Promise(() => undefined))
    mockedGetPostsByStatus.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando resumen...')
  })

  it('renders dashboard metrics and quick actions', async () => {
    mockedGetSummary.mockResolvedValue(summary())
    mockedGetPostsByStatus.mockResolvedValue(postsResponse())

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Panel administrativo')
    expect(wrapper.text()).toContain('Preguntas pendientes')
    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('Publicaciones visibles')
    expect(wrapper.text()).toContain('7')
    expect(wrapper.text()).toContain('Visitas hoy')
    expect(wrapper.text()).toContain('4')
    expect(wrapper.text()).toContain('Hashtags')
    expect(wrapper.text()).toContain('Enlaces')

    expect(mockedGetPostsByStatus).toHaveBeenCalledWith('PUBLISHED', 0, 1)
    expect(
      wrapper
        .findAllComponents(RouterLinkStub)
        .some((link) => linkToObject(link.props('to')).name === 'admin-analytics'),
    ).toBe(true)
  })

  it('renders a friendly error if summary loading fails', async () => {
    mockedGetSummary.mockRejectedValue(new Error('Network'))
    mockedGetPostsByStatus.mockResolvedValue(postsResponse())

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar el resumen del panel.')
  })

  it('signs out and redirects when admin authorization fails', async () => {
    mockedGetSummary.mockRejectedValue(new Error('Forbidden'))
    mockedGetPostsByStatus.mockResolvedValue(postsResponse())
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
  return mount(AdminView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })
}

function linkToObject(to: unknown): Record<string, unknown> {
  return typeof to === 'object' && to !== null ? (to as Record<string, unknown>) : {}
}

function summary(): AdminAnalyticsSummary {
  return {
    totalVisits: 120,
    visitsToday: 4,
    visitsThisWeek: 18,
    visitsThisMonth: 51,
    mostViewedSections: [],
    mostViewedWorkshop: null,
    mostViewedExam: null,
    mostViewedPosts: [],
    questions: {
      total: 8,
      pending: 2,
      published: 3,
    },
    dailyVisits: [],
  }
}

function postsResponse(): AdminPostsResponse {
  return {
    items: [],
    page: 0,
    size: 1,
    totalElements: 7,
    totalPages: 7,
  }
}
