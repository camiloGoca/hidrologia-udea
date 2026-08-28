import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import PublicLayout from '@/layouts/PublicLayout.vue'
import { getPublicVisitCount, recordSiteVisit } from '@/services/api/analyticsService'

vi.mock('@/services/api/analyticsService', () => ({
  recordSiteVisit: vi.fn<() => Promise<void>>(),
  getPublicVisitCount: vi.fn<() => Promise<{ visits: number }>>(),
}))

const mockedRecordSiteVisit = vi.mocked(recordSiteVisit)
const mockedGetPublicVisitCount = vi.mocked(getPublicVisitCount)

describe('PublicLayout', () => {
  beforeEach(() => {
    mockedRecordSiteVisit.mockReset()
    mockedGetPublicVisitCount.mockReset()
    mockedRecordSiteVisit.mockResolvedValue()
    mockedGetPublicVisitCount.mockResolvedValue({ visits: 15 })
  })

  it('records one public site visit and renders the public counter', async () => {
    const wrapper = mountLayout()
    await flushPromises()

    expect(mockedRecordSiteVisit).toHaveBeenCalledTimes(1)
    expect(mockedGetPublicVisitCount).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Visitas al sitio: 15')
  })

  it('keeps the public layout available if analytics fails', async () => {
    mockedRecordSiteVisit.mockRejectedValue(new Error('Network'))
    mockedGetPublicVisitCount.mockRejectedValue(new Error('Network'))

    const wrapper = mountLayout()
    await flushPromises()

    expect(wrapper.text()).toContain('Contenido público')
    expect(wrapper.text()).not.toContain('Visitas al sitio:')
  })
})

function mountLayout() {
  return mount(PublicLayout, {
    global: {
      stubs: {
        AppHeader: { template: '<header>Hidrología UdeA</header>' },
        RouterView: { template: '<main>Contenido público</main>' },
        AppFooter: {
          props: ['siteVisits'],
          template:
            '<footer><span v-if="typeof siteVisits === \'number\'">Visitas al sitio: {{ siteVisits }}</span></footer>',
        },
      },
    },
  })
}
