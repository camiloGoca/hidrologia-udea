import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '@/services/api/httpClient'
import { getSections } from '@/services/api/sectionService'
import type { Section } from '@/types/section'

vi.mock('@/services/api/httpClient', () => ({
  httpClient: {
    get: vi.fn<(url: string) => Promise<AxiosResponse<Section[]>>>(),
  },
}))

const mockedGet = vi.mocked(httpClient.get)

describe('sectionService', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  it('calls the sections endpoint and returns typed data', async () => {
    const sections: Section[] = [
      {
        id: 1,
        type: 'TALLER',
        name: 'Taller 1',
        slug: 'taller-1',
        description: 'Morfometría de cuencas',
        displayOrder: 1,
      },
    ]

    mockedGet.mockResolvedValue({ data: sections } as AxiosResponse<Section[]>)

    await expect(getSections()).resolves.toEqual(sections)
    expect(mockedGet).toHaveBeenCalledWith('/sections')
  })
})
