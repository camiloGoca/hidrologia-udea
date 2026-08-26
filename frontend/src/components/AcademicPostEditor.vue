<script setup lang="ts">
import { watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'

import type { PostContentDocument } from '@/types/postContent'
import { emptyPostContentDocument } from '@/types/postContent'
import { createAcademicPostEditorExtensions } from '@/utils/academicPostEditorExtensions'

const props = defineProps<{
  id: string
  modelValue: PostContentDocument
}>()

const emit = defineEmits<{
  'update:modelValue': [value: PostContentDocument]
}>()

let isApplyingExternalContent = false
let lastEmittedContent = ''

const editor = useEditor({
  content: props.modelValue ?? emptyPostContentDocument(),
  extensions: createAcademicPostEditorExtensions(),
  editorProps: {
    attributes: {
      id: props.id,
      class:
        'min-h-80 rounded-b-2xl border-x border-b border-slate-300 bg-white px-4 py-4 text-base leading-7 text-slate-950 outline-none focus:ring-4 focus:ring-emerald-100',
      'aria-label': 'Contenido de la publicación',
    },
  },
  onUpdate({ editor }) {
    if (isApplyingExternalContent) {
      return
    }

    const content = editor.getJSON() as PostContentDocument
    lastEmittedContent = JSON.stringify(content)
    emit('update:modelValue', content)
  },
})

watch(
  () => props.modelValue,
  (value) => {
    if (!editor.value) {
      return
    }

    const current = JSON.stringify(editor.value.getJSON())
    const incoming = JSON.stringify(value ?? emptyPostContentDocument())
    if (incoming === lastEmittedContent) {
      return
    }

    if (current !== incoming) {
      isApplyingExternalContent = true
      try {
        editor.value.commands.setContent(value ?? emptyPostContentDocument(), {
          emitUpdate: false,
        })
      } finally {
        isApplyingExternalContent = false
      }
    }
  },
)

function setParagraph() {
  editor.value?.chain().focus().setParagraph().run()
}

function setSubtitle() {
  editor.value?.chain().focus().toggleHeading({ level: 2 }).run()
}

function toggleLink() {
  const currentHref = editor.value?.getAttributes('link').href as string | undefined
  const href = window.prompt('URL del enlace', currentHref ?? '')

  if (href === null) {
    return
  }

  if (href.trim() === '') {
    editor.value?.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }

  editor.value?.chain().focus().extendMarkRange('link').setLink({ href: href.trim() }).run()
}
</script>

<template>
  <div class="mt-3">
    <div
      class="flex flex-wrap gap-2 rounded-t-2xl border border-slate-300 bg-slate-50 p-3"
      aria-label="Herramientas del editor"
    >
      <button type="button" class="editor-button" @click="setParagraph">Párrafo</button>
      <button
        type="button"
        class="editor-button"
        :aria-pressed="editor?.isActive('heading', { level: 2 })"
        @click="setSubtitle"
      >
        Subtítulo
      </button>
      <button
        type="button"
        class="editor-button"
        :aria-pressed="editor?.isActive('bold')"
        @click="editor?.chain().focus().toggleBold().run()"
      >
        Negrita
      </button>
      <button
        type="button"
        class="editor-button"
        :aria-pressed="editor?.isActive('italic')"
        @click="editor?.chain().focus().toggleItalic().run()"
      >
        Cursiva
      </button>
      <button
        type="button"
        class="editor-button"
        :aria-pressed="editor?.isActive('underline')"
        @click="editor?.chain().focus().toggleUnderline().run()"
      >
        Subrayado
      </button>
      <button
        type="button"
        class="editor-button"
        :aria-pressed="editor?.isActive('bulletList')"
        @click="editor?.chain().focus().toggleBulletList().run()"
      >
        Lista con viñetas
      </button>
      <button
        type="button"
        class="editor-button"
        :aria-pressed="editor?.isActive('orderedList')"
        @click="editor?.chain().focus().toggleOrderedList().run()"
      >
        Lista numerada
      </button>
      <button
        type="button"
        class="editor-button"
        :aria-pressed="editor?.isActive('blockquote')"
        @click="editor?.chain().focus().toggleBlockquote().run()"
      >
        Cita
      </button>
      <button
        type="button"
        class="editor-button"
        :aria-pressed="editor?.isActive('link')"
        @click="toggleLink"
      >
        Enlace
      </button>
      <button type="button" class="editor-button" @click="editor?.chain().focus().undo().run()">
        Deshacer
      </button>
      <button type="button" class="editor-button" @click="editor?.chain().focus().redo().run()">
        Rehacer
      </button>
    </div>

    <EditorContent :editor="editor" />
  </div>
</template>

<style scoped>
.editor-button {
  border-radius: 9999px;
  border: 1px solid rgb(203 213 225);
  background: white;
  padding: 0.5rem 0.8rem;
  font-size: 0.8rem;
  font-weight: 900;
  color: rgb(15 23 42);
  transition:
    background-color 150ms ease,
    border-color 150ms ease,
    color 150ms ease;
}

.editor-button:hover,
.editor-button[aria-pressed='true'] {
  border-color: rgb(4 120 87);
  background: rgb(4 120 87);
  color: white;
}

.editor-button:focus-visible {
  outline: 2px solid rgb(4 120 87);
  outline-offset: 3px;
}
</style>
