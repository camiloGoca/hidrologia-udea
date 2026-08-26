import type { Extensions } from '@tiptap/core'
import Link from '@tiptap/extension-link'
import Underline from '@tiptap/extension-underline'
import StarterKit from '@tiptap/starter-kit'

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
