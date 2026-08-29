import { defineComponent, h, type PropType, type VNode } from 'vue'

import type {
  PostContentAcademicBlockKind,
  PostContentDocument,
  PostContentHighlightKind,
  PostContentImageDisplaySize,
  PostContentMark,
  PostContentNode,
  PostContentTextAlign,
  PostContentTextColor,
  PostContentTextSize,
} from '@/types/postContent'
import type { PostImage } from '@/types/post'
import { isDirectVideoSource, tiktokEmbedUrl, youtubeEmbedUrl } from '@/utils/videoEmbeds'

const SAFE_PROTOCOLS = new Set(['http:', 'https:', 'mailto:'])
const TEXT_ALIGN_CLASSES: Record<PostContentTextAlign, string> = {
  left: 'text-left',
  center: 'text-center',
  right: 'text-right',
  justify: 'text-justify',
}
const TEXT_SIZE_CLASSES: Record<PostContentTextSize, string> = {
  small: 'text-base',
  normal: 'text-lg',
  large: 'text-xl',
}
const TEXT_COLOR_CLASSES: Record<PostContentTextColor, string> = {
  default: 'text-slate-800',
  institutional: 'text-emerald-800',
  blue: 'text-sky-800',
  muted: 'text-slate-600',
  danger: 'text-red-700',
}
const HIGHLIGHT_CLASSES: Record<PostContentHighlightKind, string> = {
  note: 'rounded-md bg-amber-100 px-1 py-0.5 text-slate-950',
  important: 'rounded-md bg-red-100 px-1 py-0.5 text-red-950',
}
const ACADEMIC_BLOCK_CLASSES: Record<PostContentAcademicBlockKind, string> = {
  note: 'border-emerald-200 bg-emerald-50 text-emerald-950',
  example: 'border-sky-200 bg-sky-50 text-sky-950',
  important: 'border-orange-200 bg-orange-50 text-orange-950',
}
const IMAGE_DISPLAY_SIZE_CLASSES: Record<PostContentImageDisplaySize, string> = {
  small: 'w-fit max-w-sm',
  medium: 'w-fit max-w-2xl',
  large: 'w-full max-w-full',
}

export default defineComponent({
  name: 'PostContentRenderer',
  props: {
    document: {
      type: Object as PropType<PostContentDocument>,
      required: true,
    },
    images: {
      type: Array as PropType<PostImage[]>,
      default: () => [],
    },
  },
  setup(props, { attrs }) {
    return () =>
      h(
        'div',
        {
          ...attrs,
          class: [
            'post-content space-y-5 rounded-3xl border border-slate-200 bg-white p-7 text-lg leading-8 text-slate-800 shadow-sm sm:p-10',
            attrs.class,
          ],
        },
        renderChildren(props.document.content ?? [], imageMap(props.images)),
      )
  },
})

function renderChildren(nodes: PostContentNode[], images: Map<number, PostImage>): VNode[] {
  return nodes.map((node, index) => renderNode(node, index, images)).filter((node): node is VNode => Boolean(node))
}

function renderNode(node: PostContentNode, index: number, images: Map<number, PostImage>): VNode | null {
  const key = `${node.type}-${index}`
  const children = renderChildren(node.content ?? [], images)

  switch (node.type) {
    case 'paragraph':
      return h('p', { key, class: textAlignClass(node) }, children.length ? children : '\u00a0')
    case 'heading':
      return h(headingTag(node), { key, class: headingClass(node) }, children)
    case 'bulletList':
      return h('ul', { key, class: 'list-disc space-y-2 pl-6' }, children)
    case 'orderedList':
      return h('ol', { key, class: 'list-decimal space-y-2 pl-6', start: node.attrs?.start }, children)
    case 'listItem':
      return h('li', { key }, children)
    case 'blockquote':
      return h(
        'blockquote',
        {
          key,
          class: 'rounded-2xl border-l-4 border-emerald-700 bg-emerald-50 px-5 py-4 font-semibold text-emerald-950',
        },
        children,
      )
    case 'academicBlock':
      return renderAcademicBlock(node, key, children)
    case 'image':
      return renderImage(node, key, images)
    case 'video':
      return renderVideo(node, key)
    case 'text':
      return renderText(node, key)
    case 'hardBreak':
      return h('br', { key })
    default:
      return null
  }
}

function renderImage(node: PostContentNode, key: string, images: Map<number, PostImage>): VNode {
  const postImageId = node.attrs?.postImageId
  const image = typeof postImageId === 'number' ? images.get(postImageId) : undefined
  if (!image) {
    return h(
      'figure',
      {
        key,
        class:
          'rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-5 py-6 text-center text-sm font-bold text-slate-500',
      },
      'Imagen no disponible',
    )
  }

  const caption = node.attrs?.caption?.trim()
  const displaySize = imageDisplaySize(node)
  const imageWidthClass = displaySize === 'large' ? 'w-full' : 'w-auto'

  return h(
    'figure',
    {
      key,
      class: [
        'mx-auto rounded-3xl border border-slate-200 bg-slate-50 p-3 shadow-sm',
        IMAGE_DISPLAY_SIZE_CLASSES[displaySize],
      ],
    },
    [
      h('img', {
        src: image.secureUrl,
        alt: image.altText,
        width: image.width,
        height: image.height,
        class: ['h-auto max-w-full rounded-2xl object-contain', imageWidthClass],
        loading: 'lazy',
      }),
      caption
        ? h('figcaption', { class: 'px-3 py-3 text-center text-sm font-bold leading-6 text-slate-600' }, caption)
        : null,
    ],
  )
}

function renderVideo(node: PostContentNode, key: string): VNode {
  const provider = node.attrs?.provider
  if (provider === 'youtube') {
    const src = youtubeEmbedUrl(node.attrs?.videoId)
    if (!src) {
      return renderUnavailableVideo(key)
    }

    return h(
      'figure',
      { key, class: 'overflow-hidden rounded-3xl border border-slate-200 bg-slate-950 shadow-sm' },
      [
        h('iframe', {
          src,
          title: 'Video de YouTube',
          class: 'aspect-video w-full',
          loading: 'lazy',
          allow: 'accelerometer; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share',
          allowfullscreen: true,
        }),
      ],
    )
  }

  if (provider === 'tiktok') {
    const src = tiktokEmbedUrl(node.attrs?.videoId)
    if (!src) {
      return renderUnavailableVideo(key)
    }

    return h(
      'figure',
      { key, class: 'mx-auto overflow-hidden rounded-3xl border border-slate-200 bg-slate-950 shadow-sm sm:max-w-sm' },
      [
        h('iframe', {
          src,
          title: 'Video de TikTok',
          class: 'aspect-[9/16] w-full',
          loading: 'lazy',
          allow: 'encrypted-media; picture-in-picture',
          allowfullscreen: true,
        }),
      ],
    )
  }

  if (provider === 'direct' && isDirectVideoSource(node.attrs?.sourceUrl)) {
    return h(
      'figure',
      { key, class: 'overflow-hidden rounded-3xl border border-slate-200 bg-slate-950 p-2 shadow-sm' },
      [
        h('video', {
          src: node.attrs?.sourceUrl,
          class: 'h-auto w-full rounded-2xl',
          controls: true,
          preload: 'metadata',
          playsinline: true,
        }),
      ],
    )
  }

  return renderUnavailableVideo(key)
}

function renderUnavailableVideo(key: string): VNode {
  return h(
    'figure',
    {
      key,
      class:
        'rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-5 py-6 text-center text-sm font-bold text-slate-500',
    },
    'Video no disponible',
  )
}

function renderAcademicBlock(node: PostContentNode, key: string, children: VNode[]): VNode {
  const kind = node.attrs?.kind ?? 'note'

  return h(
    'section',
    {
      key,
      class: [
        'rounded-2xl border px-5 py-4 shadow-sm',
        ACADEMIC_BLOCK_CLASSES[kind] ?? ACADEMIC_BLOCK_CLASSES.note,
      ],
    },
    [
      h('p', { class: 'mb-2 text-xs font-black uppercase text-current' }, academicBlockLabel(kind)),
      ...children,
    ],
  )
}

function renderText(node: PostContentNode, key: string): VNode {
  return (node.marks ?? []).reduce<VNode>(
    (child, mark) => renderMark(mark, child),
    h('span', { key }, node.text ?? ''),
  )
}

function renderMark(mark: PostContentMark, child: VNode): VNode {
  switch (mark.type) {
    case 'bold':
      return h('strong', {}, [child])
    case 'italic':
      return h('em', {}, [child])
    case 'underline':
      return h('u', {}, [child])
    case 'link':
      return renderLink(mark, child)
    case 'textSize':
      return h('span', { class: textSizeClass(mark) }, [child])
    case 'textColor':
      return h('span', { class: textColorClass(mark) }, [child])
    case 'highlight':
      return h('mark', { class: highlightClass(mark) }, [child])
    default:
      return child
  }
}

function renderLink(mark: PostContentMark, child: VNode): VNode {
  const href = mark.attrs?.href ?? ''
  if (!isSafeLink(href)) {
    return child
  }

  const isExternal = href.startsWith('http://') || href.startsWith('https://') || href.startsWith('mailto:')

  return h(
    'a',
    {
      href,
      target: isExternal ? '_blank' : undefined,
      rel: isExternal ? 'noopener noreferrer' : undefined,
      class:
        'font-bold text-sky-800 underline decoration-emerald-300 underline-offset-4 hover:text-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700',
    },
    [child],
  )
}

function headingTag(node: PostContentNode): 'h2' | 'h3' {
  return node.attrs?.level === 3 ? 'h3' : 'h2'
}

function headingClass(node: PostContentNode): string[] {
  const base =
    node.attrs?.level === 3
      ? 'text-2xl font-black leading-tight text-emerald-900'
      : 'text-3xl font-black leading-tight text-sky-950'

  return [base, textAlignClass(node)]
}

function textAlignClass(node: PostContentNode): string {
  return TEXT_ALIGN_CLASSES[node.attrs?.textAlign ?? 'left']
}

function textSizeClass(mark: PostContentMark): string {
  return TEXT_SIZE_CLASSES[mark.attrs?.size ?? 'normal']
}

function textColorClass(mark: PostContentMark): string {
  return TEXT_COLOR_CLASSES[mark.attrs?.color ?? 'default']
}

function highlightClass(mark: PostContentMark): string {
  return HIGHLIGHT_CLASSES[mark.attrs?.kind ?? 'note']
}

function academicBlockLabel(kind: PostContentAcademicBlockKind): string {
  switch (kind) {
    case 'example':
      return 'Ejemplo'
    case 'important':
      return 'Importante'
    case 'note':
      return 'Nota'
  }
}

function imageMap(images: PostImage[]): Map<number, PostImage> {
  return new Map(images.map((image) => [image.id, image]))
}

function imageDisplaySize(node: PostContentNode): PostContentImageDisplaySize {
  const displaySize = node.attrs?.displaySize

  return displaySize === 'small' || displaySize === 'large' ? displaySize : 'medium'
}

function isSafeLink(href: string): boolean {
  if (href.startsWith('/') && !href.startsWith('//')) {
    return true
  }
  if (href.startsWith('#')) {
    return true
  }

  try {
    return SAFE_PROTOCOLS.has(new URL(href).protocol)
  } catch {
    return false
  }
}
