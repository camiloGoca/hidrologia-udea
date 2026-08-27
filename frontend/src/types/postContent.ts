export type PostContentTextSize = 'small' | 'normal' | 'large'
export type PostContentTextColor = 'default' | 'institutional' | 'blue' | 'muted' | 'danger'
export type PostContentHighlightKind = 'note' | 'important'
export type PostContentTextAlign = 'left' | 'center' | 'right' | 'justify'
export type PostContentAcademicBlockKind = 'note' | 'example' | 'important'
export type PostContentImageDisplaySize = 'small' | 'medium' | 'large'

export type PostContentMarkType =
  | 'bold'
  | 'italic'
  | 'underline'
  | 'link'
  | 'textSize'
  | 'textColor'
  | 'highlight'

export interface PostContentMark {
  type: PostContentMarkType
  attrs?: {
    href?: string
    target?: string | null
    rel?: string | null
    class?: string | null
    title?: string | null
    size?: PostContentTextSize
    color?: PostContentTextColor
    kind?: PostContentHighlightKind
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
  | 'academicBlock'
  | 'image'

export interface PostContentNode {
  type: PostContentNodeType
  text?: string
  attrs?: {
    level?: 2 | 3
    start?: number
    type?: string | null
    textAlign?: PostContentTextAlign | null
    kind?: PostContentAcademicBlockKind
    postImageId?: number
    caption?: string | null
    displaySize?: PostContentImageDisplaySize
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
