import { defineComponent, h, type PropType, type VNode } from 'vue'

import type { PostContentDocument, PostContentMark, PostContentNode } from '@/types/postContent'

const SAFE_PROTOCOLS = new Set(['http:', 'https:', 'mailto:'])

export default defineComponent({
  name: 'PostContentRenderer',
  props: {
    document: {
      type: Object as PropType<PostContentDocument>,
      required: true,
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
        renderChildren(props.document.content ?? []),
      )
  },
})

function renderChildren(nodes: PostContentNode[]): VNode[] {
  return nodes.map((node, index) => renderNode(node, index)).filter((node): node is VNode => Boolean(node))
}

function renderNode(node: PostContentNode, index: number): VNode | null {
  const key = `${node.type}-${index}`
  const children = renderChildren(node.content ?? [])

  switch (node.type) {
    case 'paragraph':
      return h('p', { key }, children.length ? children : '\u00a0')
    case 'heading':
      return h(headingTag(node), { key, class: 'font-black leading-tight text-sky-950' }, children)
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
    case 'text':
      return renderText(node, key)
    case 'hardBreak':
      return h('br', { key })
    default:
      return null
  }
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
