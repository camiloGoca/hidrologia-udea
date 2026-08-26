import { mount } from '@vue/test-utils'
import { Editor } from '@tiptap/core'
import { afterEach, describe, expect, it, vi } from 'vitest'

import AcademicPostEditor from '@/components/AcademicPostEditor.vue'
import type { PostContentDocument } from '@/types/postContent'
import { emptyPostContentDocument } from '@/types/postContent'
import { createAcademicPostEditorExtensions } from '@/utils/academicPostEditorExtensions'

describe('AcademicPostEditor', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders the EP1 toolbar controls', () => {
    const wrapper = mount(AcademicPostEditor, {
      props: {
        id: 'post-content',
        modelValue: emptyPostContentDocument(),
      },
    })

    expect(wrapper.find('[aria-label="Herramientas del editor"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Párrafo')
    expect(wrapper.text()).toContain('Subtítulo')
    expect(wrapper.text()).toContain('Negrita')
    expect(wrapper.text()).toContain('Cursiva')
    expect(wrapper.text()).toContain('Subrayado')
    expect(wrapper.text()).toContain('Lista con viñetas')
    expect(wrapper.text()).toContain('Lista numerada')
    expect(wrapper.text()).toContain('Cita')
    expect(wrapper.text()).toContain('Enlace')
    expect(wrapper.text()).toContain('Deshacer')
    expect(wrapper.text()).toContain('Rehacer')
  })

  it('configures Tiptap without duplicate link or underline extensions', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined)
    const editor = createEditor()

    const extensionNames = editor.extensionManager.extensions.map((extension) => extension.name)

    expect(extensionNames.filter((name) => name === 'link')).toHaveLength(1)
    expect(extensionNames.filter((name) => name === 'underline')).toHaveLength(1)
    expect(warn).not.toHaveBeenCalledWith(
      expect.stringContaining('Duplicate extension names found'),
    )

    editor.destroy()
  })

  it('creates the Tiptap link JSON shape expected by the backend normalizer', () => {
    const editor = createEditor()

    editor.commands.setContent({
      type: 'doc',
      content: [
        {
          type: 'paragraph',
          content: [{ type: 'text', text: 'UdeA' }],
        },
      ],
    })
    editor.commands.setTextSelection({ from: 1, to: 5 })
    editor.commands.setLink({ href: 'https://www.udea.edu.co' })

    expect(editor.getJSON()).toEqual({
      type: 'doc',
      content: [
        {
          type: 'paragraph',
          content: [
            {
              type: 'text',
              marks: [
                {
                  type: 'link',
                  attrs: {
                    href: 'https://www.udea.edu.co',
                    target: '_blank',
                    rel: 'noopener noreferrer',
                    class: null,
                    title: null,
                  },
                },
              ],
              text: 'UdeA',
            },
          ],
        },
      ],
    })

    editor.destroy()
  })

  it('does not mutate or emit content updates when text is selected', () => {
    const onUpdate = vi.fn<() => void>()
    const editor = createEditor(textDocument(), onUpdate)
    const before = editor.getJSON()

    selectText(editor, 'no debe')

    expect(editor.getJSON()).toEqual(before)
    expect(onUpdate).not.toHaveBeenCalled()

    editor.destroy()
  })

  it('does not echo external content synchronization back to v-model', async () => {
    const wrapper = mount(AcademicPostEditor, {
      props: {
        id: 'post-content',
        modelValue: textDocument('Contenido inicial'),
      },
    })

    await wrapper.setProps({
      modelValue: textDocument('Contenido actualizado desde el servidor'),
    })

    expect(wrapper.emitted('update:modelValue')).toBeUndefined()

    wrapper.unmount()
  })

  it('ignores the parent echo of its own editor update', async () => {
    const wrapper = mount(AcademicPostEditor, {
      props: {
        id: 'post-content',
        modelValue: textDocument('Contenido inicial'),
      },
    })
    const editor = getWrapperEditor(wrapper.vm)

    expect(editor).not.toBeNull()

    if (!editor) {
      throw new Error('AcademicPostEditor did not expose the Tiptap editor in this test.')
    }

    editor.view.dispatch(editor.state.tr.insertText(' actualizado', 1))

    const updates = wrapper.emitted('update:modelValue') ?? []
    const firstUpdate = updates[0]

    expect(updates).toHaveLength(1)
    expect(firstUpdate).toBeDefined()

    await wrapper.setProps({
      modelValue: firstUpdate?.[0] as PostContentDocument,
    })

    expect(wrapper.emitted('update:modelValue')).toHaveLength(1)

    wrapper.unmount()
  })

  it('does not throw a TransformError when pressing Enter over a partial paragraph selection', () => {
    expect(() => pressEnterOnSelection(textDocument())).not.toThrow()
  })

  it('does not throw a TransformError when pressing Enter over a partial heading selection', () => {
    expect(() =>
      pressEnterOnSelection({
        type: 'doc',
        content: [
          {
            type: 'heading',
            attrs: { level: 2 },
            content: [{ type: 'text', text: 'Este texto no debe desaparecer' }],
          },
        ],
      }),
    ).not.toThrow()
  })

  it('does not throw a TransformError when pressing Enter over a partial list item selection', () => {
    expect(() =>
      pressEnterOnSelection({
        type: 'doc',
        content: [
          {
            type: 'bulletList',
            content: [
              {
                type: 'listItem',
                content: [
                  {
                    type: 'paragraph',
                    content: [{ type: 'text', text: 'Este texto no debe desaparecer' }],
                  },
                ],
              },
            ],
          },
        ],
      }),
    ).not.toThrow()
  })

  it('does not throw a TransformError when pressing Enter over a partial blockquote selection', () => {
    expect(() =>
      pressEnterOnSelection({
        type: 'doc',
        content: [
          {
            type: 'blockquote',
            content: [
              {
                type: 'paragraph',
                content: [{ type: 'text', text: 'Este texto no debe desaparecer' }],
              },
            ],
          },
        ],
      }),
    ).not.toThrow()
  })

  it('does not throw a TransformError when pressing Enter over selected marked text', () => {
    expect(() =>
      pressEnterOnSelection({
        type: 'doc',
        content: [
          {
            type: 'paragraph',
            content: [
              {
                type: 'text',
                marks: [
                  { type: 'bold' },
                  { type: 'italic' },
                  { type: 'underline' },
                  {
                    type: 'link',
                    attrs: {
                      href: 'https://www.udea.edu.co',
                      target: '_blank',
                      rel: 'noopener noreferrer',
                      class: null,
                      title: null,
                    },
                  },
                ],
                text: 'Este texto no debe desaparecer',
              },
            ],
          },
        ],
      }),
    ).not.toThrow()
  })
})

function createEditor(
  content: PostContentDocument = emptyPostContentDocument(),
  onUpdate = vi.fn<() => void>(),
) {
  return new Editor({
    extensions: createAcademicPostEditorExtensions(),
    content,
    onUpdate,
  })
}

function getWrapperEditor(vm: unknown): Editor | null {
  const exposedEditor = (vm as { editor?: Editor | { value?: Editor | null } }).editor

  if (exposedEditor instanceof Editor) {
    return exposedEditor
  }

  return exposedEditor?.value ?? null
}

function textDocument(text = 'Este texto no debe desaparecer'): PostContentDocument {
  return {
    type: 'doc',
    content: [
      {
        type: 'paragraph',
        content: [{ type: 'text', text }],
      },
    ],
  }
}

function pressEnterOnSelection(content: PostContentDocument) {
  const editor = createEditor(content)

  try {
    selectText(editor, 'no debe')
    editor.commands.enter()
  } finally {
    editor.destroy()
  }
}

function selectText(editor: Editor, fragment: string) {
  let range: { from: number; to: number } | null = null

  editor.state.doc.descendants((node, pos) => {
    const text = node.text

    if (!text) {
      return true
    }

    const index = text.indexOf(fragment)

    if (index === -1) {
      return true
    }

    range = {
      from: pos + index,
      to: pos + index + fragment.length,
    }

    return false
  })

  expect(range).not.toBeNull()
  editor.commands.setTextSelection(range!)
}
