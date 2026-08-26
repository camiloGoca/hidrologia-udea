import { flushPromises, mount } from '@vue/test-utils'
import { Editor } from '@tiptap/core'
import { readFileSync } from 'node:fs'
import { afterEach, describe, expect, it, vi } from 'vitest'

import AcademicPostEditor from '@/components/AcademicPostEditor.vue'
import type { PostContentDocument, PostContentMark } from '@/types/postContent'
import { emptyPostContentDocument } from '@/types/postContent'
import { createAcademicPostEditorExtensions } from '@/utils/academicPostEditorExtensions'

describe('AcademicPostEditor', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders the EP2 toolbar controls grouped for academic editing', () => {
    const wrapper = mount(AcademicPostEditor, {
      props: {
        id: 'post-content',
        modelValue: emptyPostContentDocument(),
      },
    })

    const toolbar = wrapper.get('[aria-label^="Herramientas del editor"]')

    expect(toolbar.classes()).toContain('editor-toolbar')
    expect(wrapper.text()).toContain('Bloque')
    expect(wrapper.text()).toContain('Párrafo')
    expect(wrapper.text()).toContain('Subtítulo principal')
    expect(wrapper.text()).toContain('Subtítulo secundario')
    expect(wrapper.text()).toContain('Negrita')
    expect(wrapper.text()).toContain('Cursiva')
    expect(wrapper.text()).toContain('Subrayado')
    expect(wrapper.text()).toContain('Tamaño')
    expect(wrapper.text()).toContain('Color')
    expect(wrapper.text()).toContain('Resaltado')
    expect(wrapper.text()).toContain('Lista con viñetas')
    expect(wrapper.text()).toContain('Lista numerada')
    expect(wrapper.text()).toContain('Cita')
    expect(wrapper.text()).toContain('Bloque académico')
    expect(wrapper.text()).toContain('Alineación')
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

  it('defines sticky toolbar and editor-only link/highlight presentation styles', () => {
    const source = readFileSync('src/components/AcademicPostEditor.vue', 'utf-8')

    expect(source).toContain('.editor-toolbar')
    expect(source).toContain('position: sticky')
    expect(source).toContain('top: 1rem')
    expect(source).toContain('z-index: 20')
    expect(source).toContain(':deep(.ProseMirror a[href])')
    expect(source).toContain('text-decoration-line: underline')
    expect(source).toContain('cursor: text')
    expect(source).toContain('color: inherit')
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
          attrs: {
            textAlign: null,
          },
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

  it('creates controlled EP2 text marks and alignment tokens', () => {
    const editor = createEditor(textDocument())

    selectText(editor, 'no debe')
    editor.commands.setAcademicTextSize('large')
    editor.commands.setAcademicTextColor('blue')
    editor.commands.setAcademicTextHighlight('note')
    editor.commands.setAcademicTextAlign('center')

    expect(editor.getJSON()).toMatchObject({
      type: 'doc',
      content: [
        {
          type: 'paragraph',
          attrs: { textAlign: 'center' },
          content: [
            { type: 'text', text: 'Este texto ' },
            {
              type: 'text',
              text: 'no debe',
              marks: [
                { type: 'textSize', attrs: { size: 'large' } },
                { type: 'textColor', attrs: { color: 'blue' } },
                { type: 'highlight', attrs: { kind: 'note' } },
              ],
            },
            { type: 'text', text: ' desaparecer' },
          ],
        },
      ],
    })

    editor.destroy()
  })

  it('keeps text color when highlight is applied later', () => {
    const editor = createEditor(textDocument())

    selectText(editor, 'no debe')
    editor.commands.setAcademicTextColor('institutional')
    editor.commands.setAcademicTextHighlight('important')

    expect(selectedTextMarks(editor)).toEqual([
      { type: 'textColor', attrs: { color: 'institutional' } },
      { type: 'highlight', attrs: { kind: 'important' } },
    ])

    editor.destroy()
  })

  it('reloads content preserving combined color and highlight marks', async () => {
    const wrapper = mount(AcademicPostEditor, {
      props: {
        id: 'post-content',
        modelValue: emptyPostContentDocument(),
      },
    })
    const document = styledTextDocument()

    await wrapper.setProps({ modelValue: document })
    await flushPromises()

    expect(getWrapperEditor(wrapper.vm)?.getJSON()).toMatchObject(document)

    wrapper.unmount()
  })

  it('wraps selected content in a controlled academic block', () => {
    const editor = createEditor(textDocument())

    selectText(editor, 'no debe')
    editor.commands.setAcademicBlock('example')

    expect(editor.getJSON().content?.[0]).toMatchObject({
      type: 'academicBlock',
      attrs: { kind: 'example' },
      content: [
        {
          type: 'paragraph',
          content: [{ type: 'text', text: 'Este texto no debe desaparecer' }],
        },
      ],
    })

    editor.destroy()
  })

  it('opens an accessible link form instead of using a browser prompt', async () => {
    const prompt = vi.spyOn(window, 'prompt')
    const wrapper = mount(AcademicPostEditor, {
      props: {
        id: 'post-content',
        modelValue: textDocument('UdeA'),
      },
    })
    const editor = getWrapperEditor(wrapper.vm)

    expect(editor).not.toBeNull()

    if (!editor) {
      throw new Error('AcademicPostEditor did not expose the Tiptap editor in this test.')
    }

    const component = wrapper.vm as unknown as { openLinkForm: () => Promise<void> }
    expect(wrapper.find('button[title="Agregar o editar enlace"]').exists()).toBe(true)

    editor.commands.setTextSelection({ from: 1, to: 5 })
    await component.openLinkForm()
    await flushPromises()
    await wrapper.get('#post-content-link').setValue('https://www.udea.edu.co')
    await wrapper.get('[aria-label="Editar enlace"] button').trigger('click')

    expect(prompt).not.toHaveBeenCalled()
    expect(editor.getJSON()).toMatchObject({
      type: 'doc',
      content: [
        {
          content: [
            {
              marks: [
                {
                  type: 'link',
                  attrs: {
                    href: 'https://www.udea.edu.co',
                  },
                },
              ],
              text: 'UdeA',
            },
          ],
        },
      ],
    })

    wrapper.unmount()
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

function styledTextDocument(): PostContentDocument {
  return {
    type: 'doc',
    content: [
      {
        type: 'paragraph',
        content: [
          {
            type: 'text',
            text: 'Texto importante',
            marks: [
              { type: 'textColor', attrs: { color: 'institutional' } },
              { type: 'highlight', attrs: { kind: 'important' } },
            ],
          },
        ],
      },
    ],
  }
}

function selectedTextMarks(editor: Editor): PostContentMark[] {
  const content = (editor.getJSON() as PostContentDocument).content ?? []
  const paragraph = content[0]
  const textNode = paragraph?.content?.find((node) => node.text === 'no debe')

  return textNode?.marks ?? []
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
