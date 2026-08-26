import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import PostContentRenderer from '@/components/PostContentRenderer'
import type { PostContentDocument } from '@/types/postContent'

describe('PostContentRenderer', () => {
  it('renders EP1 nodes without v-html', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        document: {
          type: 'doc',
          content: [
            {
              type: 'heading',
              attrs: { level: 2 },
              content: [{ type: 'text', text: 'Subtítulo' }],
            },
            {
              type: 'paragraph',
              content: [{ type: 'text', text: '<strong>No HTML</strong>' }],
            },
            {
              type: 'bulletList',
              content: [
                {
                  type: 'listItem',
                  content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Elemento' }] }],
                },
              ],
            },
            {
              type: 'blockquote',
              content: [{ type: 'paragraph', content: [{ type: 'text', text: 'Cita' }] }],
            },
          ],
        } satisfies PostContentDocument,
      },
    })

    expect(wrapper.find('h2').text()).toBe('Subtítulo')
    expect(wrapper.find('ul li').text()).toBe('Elemento')
    expect(wrapper.find('blockquote').text()).toBe('Cita')
    expect(wrapper.html()).toContain('&lt;strong&gt;No HTML&lt;/strong&gt;')
    expect(wrapper.find('strong').exists()).toBe(false)
  })

  it('renders allowed marks and safe links', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        document: {
          type: 'doc',
          content: [
            {
              type: 'paragraph',
              content: [
                { type: 'text', text: 'Fuerte', marks: [{ type: 'bold' }] },
                { type: 'text', text: ' ' },
                {
                  type: 'text',
                  text: 'UdeA',
                  marks: [{ type: 'link', attrs: { href: 'https://udea.edu.co' } }],
                },
              ],
            },
          ],
        } satisfies PostContentDocument,
      },
    })

    expect(wrapper.find('strong').text()).toBe('Fuerte')
    expect(wrapper.find('a').attributes('href')).toBe('https://udea.edu.co')
    expect(wrapper.find('a').attributes('target')).toBe('_blank')
    expect(wrapper.find('a').attributes('rel')).toBe('noopener noreferrer')
  })

  it('does not render unsafe links as anchors', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        document: {
          type: 'doc',
          content: [
            {
              type: 'paragraph',
              content: [
                {
                  type: 'text',
                  text: 'No ejecutar',
                  marks: [{ type: 'link', attrs: { href: 'javascript:alert(1)' } }],
                },
              ],
            },
          ],
        } satisfies PostContentDocument,
      },
    })

    expect(wrapper.text()).toContain('No ejecutar')
    expect(wrapper.find('a').exists()).toBe(false)
  })

  it('does not render data links as anchors', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        document: {
          type: 'doc',
          content: [
            {
              type: 'paragraph',
              content: [
                {
                  type: 'text',
                  text: 'No data',
                  marks: [{ type: 'link', attrs: { href: 'data:text/html;base64,PHNjcmlwdD4=' } }],
                },
              ],
            },
          ],
        } satisfies PostContentDocument,
      },
    })

    expect(wrapper.text()).toContain('No data')
    expect(wrapper.find('a').exists()).toBe(false)
  })
})
