import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '@/services/api/httpClient'
import { getInterestingLinks } from '@/services/api/linkService'
import type { InterestingLink } from '@/types/interestingLink'

vi.mock('@/services/api/httpClient', () => ({
  httpClient: {
    get: vi.fn<(url: string) => Promise<AxiosResponse<InterestingLink[]>>>(),
  },
}))

const mockedGet = vi.mocked(httpClient.get)

describe('linkService', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  it('calls the links endpoint and returns typed data', async () => {
    const links: InterestingLink[] = [
      {
        id: 1,
        title: 'Recurso aprobado',
        description: 'Descripción pública opcional',
        url: 'https://example.edu/recurso',
        displayOrder: 10,
      },
    ]

    mockedGet.mockResolvedValue({ data: links } as AxiosResponse<InterestingLink[]>)

    await expect(getInterestingLinks()).resolves.toEqual(links)
    expect(mockedGet).toHaveBeenCalledWith('/links')
  })
})
