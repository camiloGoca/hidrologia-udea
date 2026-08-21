import { RouterLinkStub, flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { getQuestionById } from '@/services/api/adminService'
import { signOut } from '@/services/firebase/authService'
import type { AdminQuestionDetail } from '@/types/adminQuestion'
import AdminQuestionDetailView from '@/views/admin/AdminQuestionDetailView.vue'

const routerPush = vi.hoisted(() => vi.fn<(route: unknown) => void>())
const routeParams = vi.hoisted(() => ({ id: '1' as string | string[] }))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()

  return {
    ...actual,
    useRoute: () => ({ params: routeParams }),
    useRouter: () => ({ push: routerPush }),
  }
})

vi.mock('@/services/api/adminService', () => ({
  getQuestionById: vi.fn<(id: number) => Promise<AdminQuestionDetail>>(),
}))

vi.mock('@/services/api/adminErrors', () => ({
  isAdminAuthorizationError: vi.fn<(error: unknown) => boolean>(),
}))

vi.mock('@/services/firebase/authService', () => ({
  signOut: vi.fn<() => Promise<void>>(),
}))

const mockedGetQuestionById = vi.mocked(getQuestionById)
const mockedIsAdminAuthorizationError = vi.mocked(isAdminAuthorizationError)
const mockedSignOut = vi.mocked(signOut)

describe('AdminQuestionDetailView', () => {
  beforeEach(() => {
    routeParams.id = '1'
    routerPush.mockReset()
    mockedGetQuestionById.mockReset()
    mockedIsAdminAuthorizationError.mockReset()
    mockedIsAdminAuthorizationError.mockReturnValue(false)
    mockedSignOut.mockReset()
    mockedSignOut.mockResolvedValue()
  })

  it('renders loading state while the question is being fetched', () => {
    mockedGetQuestionById.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando pregunta...')
  })

  it('renders detail without image and keeps question as text', async () => {
    mockedGetQuestionById.mockResolvedValue(detail({ question: '<strong>No HTML</strong>' }))

    const wrapper = mountView()
    await flushPromises()

    expect(mockedGetQuestionById).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('Anónimo')
    expect(wrapper.text()).toContain('<strong>No HTML</strong>')
    expect(wrapper.find('strong').exists()).toBe(false)
    expect(wrapper.find('img').exists()).toBe(false)
  })

  it('renders detail with image and secure external link', async () => {
    mockedGetQuestionById.mockResolvedValue(
      detail({
        nickname: 'Goca',
        attachment: {
          secureUrl: 'https://res.cloudinary.com/demo/image/upload/question.png',
          format: 'png',
          width: 640,
          height: 480,
          bytes: 1000,
        },
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('Goca')
    expect(wrapper.get('img').attributes('alt')).toBe('Imagen adjunta a la pregunta')
    expect(wrapper.get('img').attributes('src')).toBe(
      'https://res.cloudinary.com/demo/image/upload/question.png',
    )
    expect(wrapper.get('a[target="_blank"]').attributes('rel')).toBe('noopener noreferrer')
  })

  it('renders error state for missing questions', async () => {
    mockedGetQuestionById.mockRejectedValue(new Error('Not found'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos cargar esta pregunta.')
  })

  it('signs out and redirects when admin authorization fails', async () => {
    mockedGetQuestionById.mockRejectedValue(new Error('Unauthorized'))
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
  return mount(AdminQuestionDetailView, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })
}

function detail(overrides: Partial<AdminQuestionDetail> = {}): AdminQuestionDetail {
  return {
    ...baseDetail(),
    ...overrides,
  }
}

function baseDetail(): AdminQuestionDetail {
  return {
    id: 1,
    nickname: null,
    question: 'Pregunta completa',
    status: 'PENDING' as const,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:30Z',
    section: {
      id: 1,
      type: 'TALLER' as const,
      name: 'Taller 1',
      slug: 'taller-1',
      description: null,
    },
    attachment: null,
  }
}
