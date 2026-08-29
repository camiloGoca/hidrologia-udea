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

  it('renders EP2 controlled styles and academic blocks through known classes', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        document: {
          type: 'doc',
          content: [
            {
              type: 'heading',
              attrs: { level: 3, textAlign: 'center' },
              content: [{ type: 'text', text: 'Subtitulo secundario' }],
            },
            {
              type: 'paragraph',
              attrs: { textAlign: 'justify' },
              content: [
                {
                  type: 'text',
                  text: 'Texto destacado',
                  marks: [
                    { type: 'textSize', attrs: { size: 'large' } },
                    { type: 'textColor', attrs: { color: 'institutional' } },
                    { type: 'highlight', attrs: { kind: 'important' } },
                  ],
                },
              ],
            },
            {
              type: 'academicBlock',
              attrs: { kind: 'example' },
              content: [
                {
                  type: 'paragraph',
                  content: [{ type: 'text', text: 'Ejemplo guiado' }],
                },
              ],
            },
          ],
        } satisfies PostContentDocument,
      },
    })

    expect(wrapper.find('h3').classes()).toContain('text-center')
    expect(wrapper.find('p.text-justify').text()).toContain('Texto destacado')
    expect(wrapper.find('span.text-xl').exists()).toBe(true)
    expect(wrapper.find('span.text-emerald-800').exists()).toBe(true)
    expect(wrapper.find('mark.bg-red-100').exists()).toBe(true)
    expect(wrapper.find('section.bg-sky-50').text()).toContain('Ejemplo')
    expect(wrapper.find('section.bg-sky-50').text()).toContain('Ejemplo guiado')
  })

  it('does not pass arbitrary class or style attributes from the document', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        document: {
          type: 'doc',
          content: [
            {
              type: 'paragraph',
              attrs: {
                textAlign: 'left',
                class: 'bg-red-900',
                style: 'position: fixed',
              } as never,
              content: [{ type: 'text', text: 'Texto seguro' }],
            },
          ],
        } satisfies PostContentDocument,
      },
    })

    expect(wrapper.html()).not.toContain('bg-red-900')
    expect(wrapper.html()).not.toContain('position: fixed')
    expect(wrapper.text()).toContain('Texto seguro')
  })

  it('renders image nodes from post image metadata instead of document URLs', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        images: [
          {
            id: 15,
            secureUrl: 'https://res.cloudinary.com/demo/image/upload/post.png',
            width: 900,
            height: 600,
            altText: 'Grafica de caudales',
          },
        ],
        document: {
          type: 'doc',
          content: [
            {
              type: 'image',
              attrs: {
                postImageId: 15,
                caption: 'Figura 1. Curva validada',
                secureUrl: 'javascript:alert(1)',
              } as never,
            },
          ],
        } satisfies PostContentDocument,
      },
    })

    const image = wrapper.get('img')

    expect(image.attributes('src')).toBe('https://res.cloudinary.com/demo/image/upload/post.png')
    expect(image.attributes('alt')).toBe('Grafica de caudales')
    expect(image.attributes('width')).toBe('900')
    expect(image.attributes('height')).toBe('600')
    expect(wrapper.get('figure').classes()).toContain('max-w-2xl')
    expect(wrapper.get('figcaption').text()).toBe('Figura 1. Curva validada')
    expect(wrapper.html()).not.toContain('javascript:alert')
  })

  it('maps image display size tokens to controlled renderer classes', () => {
    for (const [displaySize, expectedClass] of [
      ['small', 'max-w-sm'],
      ['medium', 'max-w-2xl'],
      ['large', 'w-full'],
    ] as const) {
      const wrapper = mount(PostContentRenderer, {
        props: {
          images: [
            {
              id: 15,
              secureUrl: 'https://res.cloudinary.com/demo/image/upload/post.png',
              width: 900,
              height: 600,
              altText: 'Grafica de caudales',
            },
          ],
          document: {
            type: 'doc',
            content: [
              {
                type: 'image',
                attrs: {
                  postImageId: 15,
                  displaySize,
                },
              },
            ],
          } satisfies PostContentDocument,
        },
      })

      expect(wrapper.get('figure').classes()).toContain(expectedClass)
      expect(wrapper.get('img').classes()).toContain('max-w-full')
      expect(wrapper.html()).not.toContain('max-w-[360px]')
      expect(wrapper.html()).not.toContain('style=')

      wrapper.unmount()
    }
  })

  it('degrades safely when image metadata is missing', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        images: [],
        document: {
          type: 'doc',
          content: [{ type: 'image', attrs: { postImageId: 99 } }],
        } satisfies PostContentDocument,
      },
    })

    expect(wrapper.text()).toContain('Imagen no disponible')
    expect(wrapper.find('img').exists()).toBe(false)
  })

  it('renders YouTube video nodes through the official embed URL', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        document: {
          type: 'doc',
          content: [
            {
              type: 'video',
              attrs: {
                provider: 'youtube',
                sourceUrl: 'https://www.youtube.com/watch?v=abc_DEF1234',
                videoId: 'abc_DEF1234',
              },
            },
          ],
        } satisfies PostContentDocument,
      },
    })

    const iframe = wrapper.get('iframe')

    expect(iframe.attributes('src')).toBe('https://www.youtube.com/embed/abc_DEF1234')
    expect(iframe.attributes('title')).toBe('Video de YouTube')
    expect(iframe.attributes('loading')).toBe('lazy')
    expect(iframe.attributes('allowfullscreen')).toBe('')
    expect(iframe.attributes('allow')).toContain('picture-in-picture')
    expect(wrapper.html()).not.toContain('watch?v=')
    expect(wrapper.html()).not.toContain('autoplay')
  })

  it('renders TikTok video nodes through the player URL', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        document: {
          type: 'doc',
          content: [
            {
              type: 'video',
              attrs: {
                provider: 'tiktok',
                sourceUrl: 'https://www.tiktok.com/@udea/video/1234567890',
                videoId: '1234567890',
              },
            },
          ],
        } satisfies PostContentDocument,
      },
    })

    const iframe = wrapper.get('iframe')

    expect(iframe.attributes('src')).toBe('https://www.tiktok.com/player/v1/1234567890')
    expect(iframe.attributes('title')).toBe('Video de TikTok')
    expect(iframe.attributes('loading')).toBe('lazy')
    expect(wrapper.html()).not.toContain('autoplay')
  })

  it('renders direct HTTPS videos with controls and no autoplay', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        document: {
          type: 'doc',
          content: [
            {
              type: 'video',
              attrs: {
                provider: 'direct',
                sourceUrl: 'https://cdn.example.edu/videos/caudal.webm',
                videoId: null,
              },
            },
          ],
        } satisfies PostContentDocument,
      },
    })

    const video = wrapper.get('video')

    expect(video.attributes('src')).toBe('https://cdn.example.edu/videos/caudal.webm')
    expect(video.attributes('controls')).toBe('')
    expect(video.attributes('preload')).toBe('metadata')
    expect(video.attributes('playsinline')).toBe('true')
    expect(wrapper.html()).not.toContain('autoplay')
  })

  it('does not create arbitrary iframes for malformed video nodes', () => {
    const wrapper = mount(PostContentRenderer, {
      props: {
        document: {
          type: 'doc',
          content: [
            {
              type: 'video',
              attrs: {
                provider: 'youtube',
                sourceUrl: 'https://youtube.com.evil.example/watch?v=abc_DEF1234',
                videoId: '<script>',
              },
            },
          ],
        } satisfies PostContentDocument,
      },
    })

    expect(wrapper.text()).toContain('Video no disponible')
    expect(wrapper.find('iframe').exists()).toBe(false)
    expect(wrapper.html()).not.toContain('youtube.com.evil')
  })
})
