import { Extension, Mark, mergeAttributes, Node, type CommandProps, type Extensions } from '@tiptap/core'
import Link from '@tiptap/extension-link'
import Underline from '@tiptap/extension-underline'
import StarterKit from '@tiptap/starter-kit'

import type {
  PostContentAcademicBlockKind,
  PostContentHighlightKind,
  PostContentTextAlign,
  PostContentTextColor,
  PostContentTextSize,
} from '@/types/postContent'

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    academicBlock: {
      setAcademicBlock: (kind: PostContentAcademicBlockKind) => ReturnType
      unsetAcademicBlock: () => ReturnType
    }
    academicTextAlign: {
      setAcademicTextAlign: (textAlign: PostContentTextAlign) => ReturnType
    }
    academicTextColor: {
      setAcademicTextColor: (color: PostContentTextColor) => ReturnType
      unsetAcademicTextColor: () => ReturnType
    }
    academicTextHighlight: {
      setAcademicTextHighlight: (kind: PostContentHighlightKind) => ReturnType
      unsetAcademicTextHighlight: () => ReturnType
    }
    academicTextSize: {
      setAcademicTextSize: (size: PostContentTextSize) => ReturnType
      unsetAcademicTextSize: () => ReturnType
    }
  }
}

const TEXT_BLOCK_TYPES = new Set(['paragraph', 'heading'])

const AcademicTextAlign = Extension.create({
  name: 'academicTextAlign',

  addGlobalAttributes() {
    return [
      {
        types: ['paragraph', 'heading'],
        attributes: {
          textAlign: {
            default: null,
            parseHTML: (element) => element.getAttribute('data-text-align'),
            renderHTML: (attributes) =>
              attributes.textAlign ? { 'data-text-align': attributes.textAlign } : {},
          },
        },
      },
    ]
  },

  addCommands() {
    return {
      setAcademicTextAlign:
        (textAlign) =>
        ({ state, dispatch }) =>
          updateSelectedTextBlocks({ state, dispatch }, { textAlign: textAlign === 'left' ? null : textAlign }),
    }
  },
})

const AcademicTextSize = Mark.create({
  name: 'textSize',

  addAttributes() {
    return {
      size: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-text-size'),
        renderHTML: (attributes) => (attributes.size ? { 'data-text-size': attributes.size } : {}),
      },
    }
  },

  parseHTML() {
    return [{ tag: 'span[data-text-size]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['span', mergeAttributes(HTMLAttributes), 0]
  },

  addCommands() {
    return {
      setAcademicTextSize:
        (size) =>
        ({ commands }) =>
          commands.setMark(this.name, { size }),
      unsetAcademicTextSize:
        () =>
        ({ commands }) =>
          commands.unsetMark(this.name),
    }
  },
})

const AcademicTextColor = Mark.create({
  name: 'textColor',

  addAttributes() {
    return {
      color: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-text-color'),
        renderHTML: (attributes) => (attributes.color ? { 'data-text-color': attributes.color } : {}),
      },
    }
  },

  parseHTML() {
    return [{ tag: 'span[data-text-color]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['span', mergeAttributes(HTMLAttributes), 0]
  },

  addCommands() {
    return {
      setAcademicTextColor:
        (color) =>
        ({ commands }) =>
          commands.setMark(this.name, { color }),
      unsetAcademicTextColor:
        () =>
        ({ commands }) =>
          commands.unsetMark(this.name),
    }
  },
})

const AcademicTextHighlight = Mark.create({
  name: 'highlight',

  addAttributes() {
    return {
      kind: {
        default: null,
        parseHTML: (element) => element.getAttribute('data-highlight-kind'),
        renderHTML: (attributes) => (attributes.kind ? { 'data-highlight-kind': attributes.kind } : {}),
      },
    }
  },

  parseHTML() {
    return [{ tag: 'mark[data-highlight-kind]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['mark', mergeAttributes(HTMLAttributes), 0]
  },

  addCommands() {
    return {
      setAcademicTextHighlight:
        (kind) =>
        ({ commands }) =>
          commands.setMark(this.name, { kind }),
      unsetAcademicTextHighlight:
        () =>
        ({ commands }) =>
          commands.unsetMark(this.name),
    }
  },
})

const AcademicBlock = Node.create({
  name: 'academicBlock',
  group: 'block',
  content: 'block+',
  defining: true,

  addAttributes() {
    return {
      kind: {
        default: 'note',
        parseHTML: (element) => element.getAttribute('data-academic-block') ?? 'note',
        renderHTML: (attributes) => ({ 'data-academic-block': attributes.kind ?? 'note' }),
      },
    }
  },

  parseHTML() {
    return [{ tag: 'section[data-academic-block]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['section', mergeAttributes(HTMLAttributes), 0]
  },

  addCommands() {
    return {
      setAcademicBlock:
        (kind) =>
        ({ commands, editor }) => {
          if (editor.isActive(this.name)) {
            return commands.updateAttributes(this.name, { kind })
          }

          return commands.wrapIn(this.name, { kind })
        },
      unsetAcademicBlock:
        () =>
        ({ commands }) =>
          commands.lift(this.name),
    }
  },
})

export function createAcademicPostEditorExtensions(): Extensions {
  return [
    StarterKit.configure({
      code: false,
      codeBlock: false,
      dropcursor: false,
      gapcursor: false,
      heading: {
        levels: [2, 3],
      },
      horizontalRule: false,
      link: false,
      strike: false,
      underline: false,
    }),
    AcademicTextAlign,
    AcademicTextSize,
    AcademicTextColor,
    AcademicTextHighlight,
    AcademicBlock,
    Underline,
    Link.configure({
      openOnClick: false,
      protocols: ['http', 'https', 'mailto'],
      HTMLAttributes: {
        rel: 'noopener noreferrer',
      },
    }),
  ]
}

function updateSelectedTextBlocks({ state, dispatch }: Pick<CommandProps, 'state' | 'dispatch'>, attrs: object) {
  const { from, to } = state.selection
  const transaction = state.tr
  let changed = false

  state.doc.nodesBetween(from, to, (node, pos) => {
    if (!TEXT_BLOCK_TYPES.has(node.type.name)) {
      return true
    }

    transaction.setNodeMarkup(pos, undefined, {
      ...node.attrs,
      ...attrs,
    })
    changed = true

    return false
  })

  if (changed && dispatch) {
    dispatch(transaction)
  }

  return changed
}
