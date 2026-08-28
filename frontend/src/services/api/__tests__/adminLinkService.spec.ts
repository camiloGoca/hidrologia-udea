import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { adminHttpClient } from '@/services/api/adminHttpClient'
import {
  createAdminLink,
  deleteAdminLink,
  getAdminLinks,
  updateAdminLink,
} from '@/services/api/adminLinkService'
import type { AdminInterestingLink, UpsertAdminInterestingLinkRequest } from '@/types/interestingLink'

vi.mock('@/services/api/adminHttpClient', () => ({
  adminHttpClient: {
    get: vi.fn<(url: string) => Promise<AxiosResponse<unknown>>>(),
    post: vi.fn<(url: string, payload: unknown) => Promise<AxiosResponse<unknown>>>(),
    patch: vi.fn<(url: string, payload: unknown) => Promise<AxiosResponse<unknown>>>(),
    delete: vi.fn<(url: string) => Promise<AxiosResponse<unknown>>>(),
  },
}))

const mockedGet = vi.mocked(adminHttpClient.get)
const mockedPost = vi.mocked(adminHttpClient.post)
const mockedPatch = vi.mocked(adminHttpClient.patch)
const mockedDelete = vi.mocked(adminHttpClient.delete)

describe('adminLinkService', () => {
  beforeEach(() => {
    mockedGet.mockReset()
    mockedPost.mockReset()
    mockedPatch.mockReset()
    mockedDelete.mockReset()
  })

  it('loads admin links through the admin http client', async () => {
    const links = [link()]
    mockedGet.mockResolvedValue({ data: links } as AxiosResponse<AdminInterestingLink[]>)

    await expect(getAdminLinks()).resolves.toEqual(links)

    expect(mockedGet).toHaveBeenCalledWith('/admin/links')
  })

  it('creates updates and deletes links through admin endpoints', async () => {
    const payload: UpsertAdminInterestingLinkRequest = {
      title: 'IDEAM',
      description: null,
      url: 'https://example.edu',
      displayOrder: 0,
      active: true,
    }
    mockedPost.mockResolvedValue({ data: link() } as AxiosResponse<AdminInterestingLink>)
    mockedPatch.mockResolvedValue({ data: link({ active: false }) } as AxiosResponse<AdminInterestingLink>)
    mockedDelete.mockResolvedValue({ data: undefined } as AxiosResponse<void>)

    await expect(createAdminLink(payload)).resolves.toEqual(link())
    await expect(updateAdminLink(1, { ...payload, active: false })).resolves.toEqual(link({ active: false }))
    await expect(deleteAdminLink(1)).resolves.toBeUndefined()

    expect(mockedPost).toHaveBeenCalledWith('/admin/links', payload)
    expect(mockedPatch).toHaveBeenCalledWith('/admin/links/1', { ...payload, active: false })
    expect(mockedDelete).toHaveBeenCalledWith('/admin/links/1')
  })
})

function link(overrides: Partial<AdminInterestingLink> = {}): AdminInterestingLink {
  return {
    id: 1,
    title: 'IDEAM',
    description: null,
    url: 'https://example.edu',
    displayOrder: 0,
    active: true,
    ...overrides,
  }
}
