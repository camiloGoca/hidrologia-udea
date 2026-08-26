export type PostContentMarkType = 'bold' | 'italic' | 'underline' | 'link'

export interface PostContentMark {
  type: PostContentMarkType
  attrs?: {
    href?: string
    target?: string | null
    rel?: string | null
    class?: string | null
    title?: string | null
  }
}

export type PostContentNodeType =
  | 'doc'
  | 'paragraph'
  | 'heading'
  | 'text'
  | 'bulletList'
  | 'orderedList'
  | 'listItem'
  | 'blockquote'
  | 'hardBreak'

export interface PostContentNode {
  type: PostContentNodeType
  text?: string
  attrs?: {
    level?: 2 | 3
    start?: number
    type?: string | null
  }
  marks?: PostContentMark[]
  content?: PostContentNode[]
}

export interface PostContentDocument {
  type: 'doc'
  content?: PostContentNode[]
}

export const emptyPostContentDocument = (): PostContentDocument => ({
  type: 'doc',
  content: [{ type: 'paragraph' }],
})
