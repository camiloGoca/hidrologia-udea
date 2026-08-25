import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { adminHttpClient } from '@/services/api/adminHttpClient'
import {
  createAdminTag,
  deleteAdminTag,
  getAdminTags,
  renameAdminTag,
} from '@/services/api/adminTagService'
import type { AdminTag } from '@/types/adminTag'

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

describe('adminTagService', () => {
  beforeEach(() => {
    mockedGet.mockReset()
    mockedPost.mockReset()
    mockedPatch.mockReset()
    mockedDelete.mockReset()
  })

  it('loads admin tags through the admin http client', async () => {
    const tags: AdminTag[] = [{ id: 1, name: 'Morfometría', slug: 'morfometria', usageCount: 2 }]
    mockedGet.mockResolvedValue({ data: tags } as AxiosResponse<AdminTag[]>)

    await expect(getAdminTags()).resolves.toEqual(tags)

    expect(mockedGet).toHaveBeenCalledWith('/admin/tags')
  })

  it('creates renames and deletes tags through admin endpoints', async () => {
    const tag: AdminTag = { id: 1, name: 'Morfometría', slug: 'morfometria', usageCount: 0 }
    mockedPost.mockResolvedValue({ data: tag } as AxiosResponse<AdminTag>)
    mockedPatch.mockResolvedValue({ data: tag } as AxiosResponse<AdminTag>)
    mockedDelete.mockResolvedValue({ data: undefined } as AxiosResponse<void>)

    await expect(createAdminTag({ name: 'Morfometría' })).resolves.toEqual(tag)
    await expect(renameAdminTag(1, { name: 'Morfometría de cuencas' })).resolves.toEqual(tag)
    await expect(deleteAdminTag(1)).resolves.toBeUndefined()

    expect(mockedPost).toHaveBeenCalledWith('/admin/tags', { name: 'Morfometría' })
    expect(mockedPatch).toHaveBeenCalledWith('/admin/tags/1', {
      name: 'Morfometría de cuencas',
    })
    expect(mockedDelete).toHaveBeenCalledWith('/admin/tags/1')
  })
})
