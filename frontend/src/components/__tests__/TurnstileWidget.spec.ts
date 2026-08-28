import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import TurnstileWidget from '@/components/TurnstileWidget.vue'

interface TurnstileRenderOptions {
  callback: (token: string) => void
  'expired-callback': () => void
  'error-callback': () => void
  sitekey: string
  action: string
}

describe('TurnstileWidget', () => {
  beforeEach(() => {
    document.head.innerHTML = ''
    vi.unstubAllGlobals()
  })

  it('renders the Turnstile widget with the configured site key and action', async () => {
    const renderOptions: { current?: TurnstileRenderOptions } = {}
    const render = vi.fn<(container: HTMLElement, options: TurnstileRenderOptions) => string>((_, options) => {
      renderOptions.current = options
      return 'widget-id'
    })
    const reset = vi.fn<(widgetId?: string) => void>()
    const remove = vi.fn<(widgetId: string) => void>()
    vi.stubGlobal('turnstile', { render, reset, remove })

    const wrapper = mount(TurnstileWidget, {
      props: {
        siteKey: 'test-site-key',
        action: 'student_question',
      },
    })
    await flushPromises()

    expect(render).toHaveBeenCalledTimes(1)
    expect(renderOptions.current).toBeDefined()
    const options = renderOptions.current!
    expect(options.sitekey).toBe('test-site-key')
    expect(options.action).toBe('student_question')

    options.callback('valid-token')
    options['expired-callback']()
    options['error-callback']()

    expect(wrapper.emitted('verified')?.[0]).toEqual(['valid-token'])
    expect(wrapper.emitted('expired')).toHaveLength(1)
    expect(wrapper.emitted('error')).toHaveLength(1)

    wrapper.vm.reset()
    expect(reset).toHaveBeenCalledWith('widget-id')

    wrapper.unmount()
    expect(remove).toHaveBeenCalledWith('widget-id')
  })

  it('loads the official script only once when the API is not available yet', async () => {
    mount(TurnstileWidget, {
      props: {
        siteKey: 'test-site-key',
        action: 'student_question',
      },
    })
    mount(TurnstileWidget, {
      props: {
        siteKey: 'test-site-key',
        action: 'student_question',
      },
    })

    const scripts = document.querySelectorAll('#cloudflare-turnstile-script')

    expect(scripts).toHaveLength(1)
    expect(scripts[0]?.getAttribute('src')).toBe('https://challenges.cloudflare.com/turnstile/v0/api.js')
  })
})
