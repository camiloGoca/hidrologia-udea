<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { Editor } from '@tiptap/core'
import { EditorContent, useEditor } from '@tiptap/vue-3'

import type {
  PostContentAcademicBlockKind,
  PostContentDocument,
  PostContentHighlightKind,
  PostContentTextAlign,
  PostContentTextColor,
  PostContentTextSize,
} from '@/types/postContent'
import { emptyPostContentDocument } from '@/types/postContent'
import { createAcademicPostEditorExtensions } from '@/utils/academicPostEditorExtensions'

type BlockOption = 'paragraph' | 'heading2' | 'heading3'
type HighlightOption = 'none' | PostContentHighlightKind
type AcademicBlockOption = 'none' | PostContentAcademicBlockKind

const props = defineProps<{
  id: string
  modelValue: PostContentDocument
}>()

const emit = defineEmits<{
  'update:modelValue': [value: PostContentDocument]
}>()

const editorVersion = ref(0)
const isEditorEmpty = ref(true)
const isLinkFormOpen = ref(false)
const linkHref = ref('')
const linkError = ref('')
const linkInput = ref<HTMLInputElement | null>(null)
let isApplyingExternalContent = false
let lastEmittedContent = ''

const editor = useEditor({
  content: props.modelValue ?? emptyPostContentDocument(),
  extensions: createAcademicPostEditorExtensions(),
  editorProps: {
    attributes: {
      id: props.id,
      class:
        'min-h-[26rem] rounded-b-3xl border-x border-b border-slate-300 bg-white px-5 py-6 text-base leading-8 text-slate-950 outline-none focus:ring-4 focus:ring-emerald-100 sm:px-8 sm:py-8',
      'aria-label': 'Contenido de la publicación',
    },
  },
  onCreate({ editor }) {
    refreshEditorState(editor)
  },
  onUpdate({ editor }) {
    refreshEditorState(editor)

    if (isApplyingExternalContent) {
      return
    }

    const content = editor.getJSON() as PostContentDocument
    lastEmittedContent = JSON.stringify(content)
    emit('update:modelValue', content)
  },
  onSelectionUpdate({ editor }) {
    refreshEditorState(editor)
  },
  onTransaction({ editor }) {
    refreshEditorState(editor)
  },
})

const selectedBlock = computed<BlockOption>(() => {
  trackEditorVersion()

  if (editor.value?.isActive('heading', { level: 2 })) {
    return 'heading2'
  }

  if (editor.value?.isActive('heading', { level: 3 })) {
    return 'heading3'
  }

  return 'paragraph'
})

const selectedTextSize = computed<PostContentTextSize>(() => {
  trackEditorVersion()

  return (editor.value?.getAttributes('textSize').size as PostContentTextSize | undefined) ?? 'normal'
})

const selectedTextColor = computed<PostContentTextColor>(() => {
  trackEditorVersion()

  return (editor.value?.getAttributes('textColor').color as PostContentTextColor | undefined) ?? 'default'
})

const selectedHighlight = computed<HighlightOption>(() => {
  trackEditorVersion()

  return (editor.value?.getAttributes('highlight').kind as PostContentHighlightKind | undefined) ?? 'none'
})

const selectedTextAlign = computed<PostContentTextAlign>(() => {
  trackEditorVersion()

  const paragraphAlign = editor.value?.getAttributes('paragraph').textAlign as PostContentTextAlign | undefined
  const headingAlign = editor.value?.getAttributes('heading').textAlign as PostContentTextAlign | undefined

  return paragraphAlign ?? headingAlign ?? 'left'
})

const selectedAcademicBlock = computed<AcademicBlockOption>(() => {
  trackEditorVersion()

  return (editor.value?.getAttributes('academicBlock').kind as PostContentAcademicBlockKind | undefined) ?? 'none'
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
        refreshEditorState(editor.value)
      } finally {
        isApplyingExternalContent = false
      }
    }
  },
)

function setBlock(value: BlockOption) {
  const chain = editor.value?.chain().focus()

  if (!chain) {
    return
  }

  if (value === 'heading2') {
    chain.toggleHeading({ level: 2 }).run()
    return
  }

  if (value === 'heading3') {
    chain.toggleHeading({ level: 3 }).run()
    return
  }

  chain.setParagraph().run()
}

function setTextSize(size: PostContentTextSize) {
  if (size === 'normal') {
    editor.value?.chain().focus().unsetAcademicTextSize().run()
    return
  }

  editor.value?.chain().focus().setAcademicTextSize(size).run()
}

function setTextColor(color: PostContentTextColor) {
  if (color === 'default') {
    editor.value?.chain().focus().unsetAcademicTextColor().run()
    return
  }

  editor.value?.chain().focus().setAcademicTextColor(color).run()
}

function setHighlight(kind: HighlightOption) {
  if (kind === 'none') {
    editor.value?.chain().focus().unsetAcademicTextHighlight().run()
    return
  }

  editor.value?.chain().focus().setAcademicTextHighlight(kind).run()
}

function setTextAlign(textAlign: PostContentTextAlign) {
  editor.value?.chain().focus().setAcademicTextAlign(textAlign).run()
}

function setAcademicBlock(kind: AcademicBlockOption) {
  if (kind === 'none') {
    editor.value?.chain().focus().unsetAcademicBlock().run()
    return
  }

  editor.value?.chain().focus().setAcademicBlock(kind).run()
}

async function openLinkForm() {
  const currentHref = editor.value?.getAttributes('link').href as string | undefined
  linkHref.value = currentHref ?? ''
  linkError.value = ''
  isLinkFormOpen.value = true
  await nextTick()
  linkInput.value?.focus()
}

function closeLinkForm() {
  isLinkFormOpen.value = false
  linkError.value = ''
}

function applyLink() {
  const href = linkHref.value.trim()

  if (!href) {
    removeLink()
    return
  }

  if (!isSafeLink(href)) {
    linkError.value = 'Usa un enlace http, https, mailto, /ruta interna o #ancla.'
    return
  }

  editor.value?.chain().focus().extendMarkRange('link').setLink({ href }).run()
  closeLinkForm()
}

function removeLink() {
  editor.value?.chain().focus().extendMarkRange('link').unsetLink().run()
  closeLinkForm()
}

function clearInlineFormatting() {
  editor.value?.chain().focus().unsetAllMarks().run()
}

function refreshEditorState(currentEditor: Editor) {
  editorVersion.value += 1
  isEditorEmpty.value = currentEditor.isEmpty
}

function trackEditorVersion() {
  return editorVersion.value
}

function selectValue(event: Event) {
  return (event.target as HTMLSelectElement).value
}

function isSafeLink(href: string): boolean {
  if (href.startsWith('/') && !href.startsWith('//')) {
    return true
  }

  if (href.startsWith('#')) {
    return true
  }

  try {
    return ['http:', 'https:', 'mailto:'].includes(new URL(href).protocol)
  } catch {
    return false
  }
}
</script>

<template>
  <div class="mt-3 rounded-3xl border border-slate-300 bg-white shadow-sm">
    <div
      class="editor-toolbar grid gap-4 rounded-t-3xl border-b border-slate-200 bg-slate-50 p-4"
      aria-label="Herramientas del editor académico"
    >
      <div class="editor-group">
        <label class="editor-label" for="post-content-block">Bloque</label>
        <select
          id="post-content-block"
          class="editor-select"
          :value="selectedBlock"
          :disabled="!editor"
          @change="setBlock(selectValue($event) as BlockOption)"
        >
          <option value="paragraph">Párrafo</option>
          <option value="heading2">Subtítulo principal</option>
          <option value="heading3">Subtítulo secundario</option>
        </select>
      </div>

      <div class="editor-toolbar-row">
        <div class="editor-group">
          <span class="editor-label">Texto</span>
          <div class="editor-control-set">
            <button
              type="button"
              class="editor-button"
              :aria-pressed="editor?.isActive('bold')"
              :disabled="!editor"
              title="Negrita"
              @click="editor?.chain().focus().toggleBold().run()"
            >
              Negrita
            </button>
            <button
              type="button"
              class="editor-button"
              :aria-pressed="editor?.isActive('italic')"
              :disabled="!editor"
              title="Cursiva"
              @click="editor?.chain().focus().toggleItalic().run()"
            >
              Cursiva
            </button>
            <button
              type="button"
              class="editor-button"
              :aria-pressed="editor?.isActive('underline')"
              :disabled="!editor"
              title="Subrayado"
              @click="editor?.chain().focus().toggleUnderline().run()"
            >
              Subrayado
            </button>
            <button
              type="button"
              class="editor-button"
              :disabled="!editor"
              title="Limpiar formato"
              @click="clearInlineFormatting"
            >
              Limpiar formato
            </button>
          </div>
        </div>

        <div class="editor-group">
          <label class="editor-label" for="post-content-size">Tamaño</label>
          <select
            id="post-content-size"
            class="editor-select"
            :value="selectedTextSize"
            :disabled="!editor"
            @change="setTextSize(selectValue($event) as PostContentTextSize)"
          >
            <option value="small">Pequeño</option>
            <option value="normal">Normal</option>
            <option value="large">Grande</option>
          </select>
        </div>

        <div class="editor-group">
          <label class="editor-label" for="post-content-color">Color</label>
          <select
            id="post-content-color"
            class="editor-select"
            :value="selectedTextColor"
            :disabled="!editor"
            @change="setTextColor(selectValue($event) as PostContentTextColor)"
          >
            <option value="default">Predeterminado</option>
            <option value="institutional">Institucional</option>
            <option value="blue">Azul</option>
            <option value="muted">Suave</option>
            <option value="danger">Énfasis</option>
          </select>
        </div>

        <div class="editor-group">
          <label class="editor-label" for="post-content-highlight">Resaltado</label>
          <select
            id="post-content-highlight"
            class="editor-select"
            :value="selectedHighlight"
            :disabled="!editor"
            @change="setHighlight(selectValue($event) as HighlightOption)"
          >
            <option value="none">Sin resaltado</option>
            <option value="note">Nota</option>
            <option value="important">Importante</option>
          </select>
        </div>
      </div>

      <div class="editor-toolbar-row">
        <div class="editor-group">
          <span class="editor-label">Estructura</span>
          <div class="editor-control-set">
            <button
              type="button"
              class="editor-button"
              :aria-pressed="editor?.isActive('bulletList')"
              :disabled="!editor"
              title="Lista con viñetas"
              @click="editor?.chain().focus().toggleBulletList().run()"
            >
              Lista con viñetas
            </button>
            <button
              type="button"
              class="editor-button"
              :aria-pressed="editor?.isActive('orderedList')"
              :disabled="!editor"
              title="Lista numerada"
              @click="editor?.chain().focus().toggleOrderedList().run()"
            >
              Lista numerada
            </button>
            <button
              type="button"
              class="editor-button"
              :aria-pressed="editor?.isActive('blockquote')"
              :disabled="!editor"
              title="Cita"
              @click="editor?.chain().focus().toggleBlockquote().run()"
            >
              Cita
            </button>
          </div>
        </div>

        <div class="editor-group">
          <label class="editor-label" for="post-content-academic-block">Bloque académico</label>
          <select
            id="post-content-academic-block"
            class="editor-select"
            :value="selectedAcademicBlock"
            :disabled="!editor"
            @change="setAcademicBlock(selectValue($event) as AcademicBlockOption)"
          >
            <option value="none">Sin bloque</option>
            <option value="note">Nota</option>
            <option value="example">Ejemplo</option>
            <option value="important">Importante</option>
          </select>
        </div>

        <div class="editor-group">
          <label class="editor-label" for="post-content-align">Alineación</label>
          <select
            id="post-content-align"
            class="editor-select"
            :value="selectedTextAlign"
            :disabled="!editor"
            @change="setTextAlign(selectValue($event) as PostContentTextAlign)"
          >
            <option value="left">Izquierda</option>
            <option value="center">Centro</option>
            <option value="right">Derecha</option>
            <option value="justify">Justificado</option>
          </select>
        </div>

        <div class="editor-group">
          <span class="editor-label">Otros</span>
          <div class="editor-control-set">
            <button
              type="button"
              class="editor-button"
              :aria-pressed="isLinkFormOpen || editor?.isActive('link')"
              :disabled="!editor"
              title="Agregar o editar enlace"
              @mousedown.prevent="openLinkForm"
              @click="openLinkForm"
            >
              Enlace
            </button>
            <button
              type="button"
              class="editor-button"
              :disabled="!editor"
              title="Deshacer"
              @click="editor?.chain().focus().undo().run()"
            >
              Deshacer
            </button>
            <button
              type="button"
              class="editor-button"
              :disabled="!editor"
              title="Rehacer"
              @click="editor?.chain().focus().redo().run()"
            >
              Rehacer
            </button>
          </div>
        </div>
      </div>

      <div
        v-if="isLinkFormOpen"
        class="grid gap-3 rounded-2xl border border-sky-100 bg-white p-4 sm:grid-cols-[1fr_auto_auto]"
        aria-label="Editar enlace"
        role="group"
      >
        <div>
          <label class="editor-label" for="post-content-link">URL del enlace</label>
          <input
            id="post-content-link"
            ref="linkInput"
            v-model="linkHref"
            type="text"
            inputmode="url"
            class="mt-2 w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm font-bold text-slate-950 outline-none focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
            placeholder="https://udea.edu.co"
            aria-describedby="post-content-link-help"
            @keydown.enter.prevent="applyLink"
          />
          <p id="post-content-link-help" class="mt-2 text-xs font-bold text-slate-600">
            Se permiten http, https, mailto, rutas internas y anclas.
          </p>
          <p v-if="linkError" class="mt-2 text-sm font-bold text-red-800" role="alert">
            {{ linkError }}
          </p>
        </div>
        <button type="button" class="editor-button editor-button-primary" @click="applyLink">
          Aplicar
        </button>
        <button type="button" class="editor-button" @click="removeLink">Quitar enlace</button>
      </div>
    </div>

    <div class="relative">
      <p
        v-if="isEditorEmpty"
        class="pointer-events-none absolute left-5 top-6 max-w-lg text-base font-semibold leading-7 text-slate-400 sm:left-8 sm:top-8"
      >
        Escribe una explicación clara: procedimiento, idea clave o solución para estudiantes.
      </p>
      <EditorContent :editor="editor" />
    </div>
  </div>
</template>

<style scoped>
.editor-toolbar-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
  align-items: end;
}

.editor-toolbar {
  position: sticky;
  top: 1rem;
  z-index: 20;
  box-shadow: 0 12px 26px rgb(15 23 42 / 0.08);
}

.editor-group {
  display: grid;
  gap: 0.45rem;
}

.editor-label {
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
  color: rgb(4 120 87);
}

.editor-control-set {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.editor-select,
.editor-button {
  min-height: 2.5rem;
  border-radius: 9999px;
  border: 1px solid rgb(203 213 225);
  background: white;
  padding: 0.55rem 0.85rem;
  font-size: 0.8rem;
  font-weight: 900;
  color: rgb(15 23 42);
  transition:
    background-color 150ms ease,
    border-color 150ms ease,
    box-shadow 150ms ease,
    color 150ms ease;
}

.editor-select {
  padding-right: 2rem;
}

.editor-button:hover,
.editor-button[aria-pressed='true'],
.editor-button-primary {
  border-color: rgb(4 120 87);
  background: rgb(4 120 87);
  color: white;
}

.editor-button:disabled,
.editor-select:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.editor-button:focus-visible,
.editor-select:focus-visible {
  outline: 2px solid rgb(4 120 87);
  outline-offset: 3px;
}

:deep(.ProseMirror) {
  min-height: 26rem;
  border-radius: 0 0 1.5rem 1.5rem;
  font-family:
    Inter,
    ui-sans-serif,
    system-ui,
    sans-serif;
}

:deep(.ProseMirror p) {
  margin: 0.85rem 0;
}

:deep(.ProseMirror h2) {
  margin: 1.6rem 0 0.75rem;
  font-size: 1.65rem;
  font-weight: 900;
  line-height: 1.2;
  color: rgb(8 47 73);
}

:deep(.ProseMirror h3) {
  margin: 1.35rem 0 0.6rem;
  font-size: 1.25rem;
  font-weight: 900;
  line-height: 1.25;
  color: rgb(6 78 59);
}

:deep(.ProseMirror ul),
:deep(.ProseMirror ol) {
  margin: 1rem 0;
  padding-left: 1.45rem;
}

:deep(.ProseMirror ul) {
  list-style: disc;
}

:deep(.ProseMirror ol) {
  list-style: decimal;
}

:deep(.ProseMirror li) {
  margin: 0.35rem 0;
}

:deep(.ProseMirror blockquote) {
  margin: 1.2rem 0;
  border-left: 4px solid rgb(4 120 87);
  border-radius: 1rem;
  background: rgb(236 253 245);
  padding: 0.8rem 1rem;
  color: rgb(6 78 59);
  font-weight: 700;
}

:deep(.ProseMirror a[href]) {
  color: rgb(3 105 161);
  font-weight: 800;
  text-decoration-line: underline;
  text-decoration-color: rgb(16 185 129);
  text-decoration-thickness: 0.14em;
  text-underline-offset: 0.24em;
  cursor: text;
}

:deep(.ProseMirror a[href]:hover) {
  color: rgb(4 120 87);
}

:deep(.ProseMirror [data-academic-block]) {
  margin: 1.25rem 0;
  border-radius: 1.25rem;
  padding: 1rem 1.15rem;
}

:deep(.ProseMirror [data-academic-block='note']) {
  border: 1px solid rgb(167 243 208);
  background: rgb(236 253 245);
}

:deep(.ProseMirror [data-academic-block='example']) {
  border: 1px solid rgb(186 230 253);
  background: rgb(240 249 255);
}

:deep(.ProseMirror [data-academic-block='important']) {
  border: 1px solid rgb(254 215 170);
  background: rgb(255 247 237);
}

:deep(.ProseMirror [data-text-align='center']) {
  text-align: center;
}

:deep(.ProseMirror [data-text-align='right']) {
  text-align: right;
}

:deep(.ProseMirror [data-text-align='justify']) {
  text-align: justify;
}

:deep(.ProseMirror [data-text-size='small']) {
  font-size: 0.9rem;
}

:deep(.ProseMirror [data-text-size='large']) {
  font-size: 1.18rem;
}

:deep(.ProseMirror [data-text-color='institutional']) {
  color: rgb(4 120 87);
}

:deep(.ProseMirror [data-text-color='blue']) {
  color: rgb(3 105 161);
}

:deep(.ProseMirror [data-text-color='muted']) {
  color: rgb(71 85 105);
}

:deep(.ProseMirror [data-text-color='danger']) {
  color: rgb(185 28 28);
}

:deep(.ProseMirror [data-highlight-kind]) {
  border-radius: 0.4rem;
  color: inherit;
  padding: 0.1rem 0.25rem;
}

:deep(.ProseMirror [data-highlight-kind='note']) {
  background: rgb(254 249 195);
}

:deep(.ProseMirror [data-highlight-kind='important']) {
  background: rgb(254 226 226);
}
</style>
