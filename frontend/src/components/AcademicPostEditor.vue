<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { Editor } from '@tiptap/core'
import { EditorContent, useEditor } from '@tiptap/vue-3'

import type { AdminPostImage } from '@/types/adminPost'
import type {
  PostContentAcademicBlockKind,
  PostContentDocument,
  PostContentHighlightKind,
  PostContentImageDisplaySize,
  PostContentTextAlign,
  PostContentTextColor,
  PostContentTextSize,
} from '@/types/postContent'
import { emptyPostContentDocument } from '@/types/postContent'
import { createAcademicPostEditorExtensions } from '@/utils/academicPostEditorExtensions'

type BlockOption = 'paragraph' | 'heading2' | 'heading3'
type HighlightOption = 'none' | PostContentHighlightKind
type AcademicBlockOption = 'none' | PostContentAcademicBlockKind
type ImageDialogMode = 'insert' | 'edit'

const props = defineProps<{
  id: string
  modelValue: PostContentDocument
  images?: AdminPostImage[]
  uploadImage?: (file: File, altText: string) => Promise<AdminPostImage>
  updateImageAltText?: (imageId: number, altText: string) => Promise<AdminPostImage>
  deleteImage?: (imageId: number) => Promise<void>
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
const isImageDialogOpen = ref(false)
const imageDialogMode = ref<ImageDialogMode>('insert')
const imageFile = ref<File | null>(null)
const imageAltText = ref('')
const imageCaption = ref('')
const imageDisplaySize = ref<PostContentImageDisplaySize>('medium')
const imageError = ref('')
const isImageSubmitting = ref(false)
const imageFileInput = ref<HTMLInputElement | null>(null)
const imageAltInput = ref<HTMLInputElement | null>(null)
let savedImageSelection: { from: number; to: number } | null = null
let selectedImagePosition: number | null = null
let selectedImageId: number | null = null
let isApplyingExternalContent = false
let lastEmittedContent = ''

const editor = useEditor({
  content: props.modelValue ?? emptyPostContentDocument(),
  extensions: createAcademicPostEditorExtensions({
    getImageById,
    onEditImage: openEditImageDialog,
  }),
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

async function openInsertImageDialog() {
  if (!editor.value || !props.uploadImage) {
    return
  }

  savedImageSelection = {
    from: editor.value.state.selection.from,
    to: editor.value.state.selection.to,
  }
  imageDialogMode.value = 'insert'
  selectedImageId = null
  selectedImagePosition = null
  imageFile.value = null
  imageAltText.value = ''
  imageCaption.value = ''
  imageDisplaySize.value = 'medium'
  imageError.value = ''
  isImageDialogOpen.value = true
  await nextTick()
  imageFileInput.value?.focus()
}

async function openEditImageDialog(imageId: number, position: number) {
  const image = getImageById(imageId)
  const currentNode = editor.value?.state.doc.nodeAt(position)
  if (!image || !currentNode) {
    return
  }

  imageDialogMode.value = 'edit'
  selectedImageId = imageId
  selectedImagePosition = position
  savedImageSelection = null
  imageFile.value = null
  imageAltText.value = image.altText
  imageCaption.value = (currentNode.attrs.caption as string | null | undefined) ?? ''
  imageDisplaySize.value = (currentNode.attrs.displaySize as PostContentImageDisplaySize | undefined) ?? 'medium'
  imageError.value = ''
  isImageDialogOpen.value = true
  editor.value?.commands.setNodeSelection(position)
  await nextTick()
  imageAltInput.value?.focus()
}

function closeImageDialog() {
  if (isImageSubmitting.value) {
    return
  }

  finishImageDialog()
}

function finishImageDialog() {
  isImageDialogOpen.value = false
  imageError.value = ''
  imageFile.value = null
}

function selectImageFile(event: Event) {
  const input = event.target as HTMLInputElement
  imageFile.value = input.files?.[0] ?? null
}

async function submitImageDialog() {
  if (isImageSubmitting.value) {
    return
  }

  imageError.value = ''
  const normalizedAltText = imageAltText.value.trim()
  const normalizedCaption = imageCaption.value.trim()
  const validationError = validateImageForm(normalizedAltText, normalizedCaption)
  if (validationError) {
    imageError.value = validationError
    return
  }

  isImageSubmitting.value = true
  try {
    if (imageDialogMode.value === 'insert') {
      await uploadAndInsertImage(normalizedAltText, normalizedCaption)
    } else {
      await updateSelectedImage(normalizedAltText, normalizedCaption, imageDisplaySize.value)
    }
    finishImageDialog()
  } catch {
    if (!imageError.value) {
      imageError.value =
        imageDialogMode.value === 'insert'
          ? 'No pudimos insertar la imagen. Intenta nuevamente.'
          : 'No pudimos actualizar la imagen. Intenta nuevamente.'
    }
  } finally {
    isImageSubmitting.value = false
  }
}

async function uploadAndInsertImage(altText: string, caption: string) {
  if (!props.uploadImage || !imageFile.value || !editor.value) {
    return
  }

  const uploadedImage = await props.uploadImage(imageFile.value, altText)
  const chain = restoreSavedImageSelection()
  if (!chain) {
    await compensateUninsertedImage(uploadedImage.id)
    throw new Error('Image editor is not available')
  }

  const inserted = chain
    .insertPostImage({
      postImageId: uploadedImage.id,
      caption: caption || null,
      displaySize: imageDisplaySize.value,
    })
    .run()

  if (!inserted) {
    await compensateUninsertedImage(uploadedImage.id)
    throw new Error('Image node could not be inserted')
  }
}

async function updateSelectedImage(
  altText: string,
  caption: string,
  displaySize: PostContentImageDisplaySize,
) {
  if (!editor.value || selectedImageId == null || selectedImagePosition == null) {
    return
  }

  const image = getImageById(selectedImageId)
  if (image && image.altText !== altText) {
    await props.updateImageAltText?.(selectedImageId, altText)
  }

  editor.value
    .chain()
    .focus()
    .setNodeSelection(selectedImagePosition)
    .updateAttributes('image', { caption: caption || null, displaySize })
    .run()
}

function restoreSavedImageSelection() {
  const currentEditor = editor.value
  if (!currentEditor || !savedImageSelection) {
    return currentEditor?.chain().focus()
  }

  const maxPosition = currentEditor.state.doc.content.size
  const from = Math.min(savedImageSelection.from, maxPosition)
  const to = Math.min(savedImageSelection.to, maxPosition)

  return currentEditor.chain().focus().setTextSelection({ from, to })
}

async function compensateUninsertedImage(imageId: number) {
  try {
    await props.deleteImage?.(imageId)
  } catch {
    imageError.value =
      'La imagen se subió, pero no se pudo insertar ni limpiar automáticamente. Guarda el contenido actual y revisa la imagen en esta publicación.'
  }
}

function validateImageForm(altText: string, caption: string): string {
  if (imageDialogMode.value === 'insert') {
    if (!imageFile.value) {
      return 'Selecciona una imagen JPEG o PNG.'
    }
    if (!['image/jpeg', 'image/png'].includes(imageFile.value.type)) {
      return 'La imagen debe estar en formato JPEG o PNG.'
    }
    if (imageFile.value.size > 5 * 1024 * 1024) {
      return 'La imagen no debe superar 5 MB.'
    }
  }

  if (!altText) {
    return 'El texto alternativo es obligatorio.'
  }
  if (altText.length > 180) {
    return 'El texto alternativo no debe superar 180 caracteres.'
  }
  if (caption.length > 240) {
    return 'El pie de imagen no debe superar 240 caracteres.'
  }

  return ''
}

function getImageById(id: number) {
  return props.images?.find((image) => image.id === id)
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
  <div class="mt-3 min-w-0 max-w-full rounded-3xl border border-slate-300 bg-white shadow-sm">
    <div
      class="editor-toolbar flex flex-wrap items-end gap-2 rounded-t-3xl border-b border-slate-200 bg-slate-50 px-3 py-2"
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
              class="editor-button editor-button-icon"
              :aria-pressed="editor?.isActive('bold')"
              :disabled="!editor"
              title="Negrita"
              @click="editor?.chain().focus().toggleBold().run()"
            >
              <span class="editor-label-sr">Negrita</span>
              <span aria-hidden="true">B</span>
            </button>
            <button
              type="button"
              class="editor-button editor-button-icon"
              :aria-pressed="editor?.isActive('italic')"
              :disabled="!editor"
              title="Cursiva"
              @click="editor?.chain().focus().toggleItalic().run()"
            >
              <span class="editor-label-sr">Cursiva</span>
              <span aria-hidden="true">I</span>
            </button>
            <button
              type="button"
              class="editor-button editor-button-icon"
              :aria-pressed="editor?.isActive('underline')"
              :disabled="!editor"
              title="Subrayado"
              @click="editor?.chain().focus().toggleUnderline().run()"
            >
              <span class="editor-label-sr">Subrayado</span>
              <span aria-hidden="true">U</span>
            </button>
          </div>
        </div>

        <details class="editor-menu">
          <summary class="editor-menu-trigger">Formato</summary>
          <div class="editor-menu-panel">
            <button
              type="button"
              class="editor-button"
              :disabled="!editor"
              title="Limpiar formato"
              @click="clearInlineFormatting"
            >
              Limpiar formato
            </button>

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
        </details>
      </div>

      <div class="editor-toolbar-row">
        <div class="editor-group">
          <span class="editor-label">Estructura</span>
          <div class="editor-control-set">
            <button
              type="button"
              class="editor-button editor-button-icon"
              :aria-pressed="editor?.isActive('bulletList')"
              :disabled="!editor"
              title="Lista con viñetas"
              @click="editor?.chain().focus().toggleBulletList().run()"
            >
              <span class="editor-label-sr">Lista con viñetas</span>
              <span aria-hidden="true">•</span>
            </button>
            <button
              type="button"
              class="editor-button editor-button-icon"
              :aria-pressed="editor?.isActive('orderedList')"
              :disabled="!editor"
              title="Lista numerada"
              @click="editor?.chain().focus().toggleOrderedList().run()"
            >
              <span class="editor-label-sr">Lista numerada</span>
              <span aria-hidden="true">1.</span>
            </button>
          </div>
        </div>

        <details class="editor-menu">
          <summary class="editor-menu-trigger">Bloque académico</summary>
          <div class="editor-menu-panel">
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
          </div>
        </details>

        <details class="editor-menu">
          <summary class="editor-menu-trigger">Alinear</summary>
          <div class="editor-menu-panel">
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
          </div>
        </details>

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
              :aria-pressed="isImageDialogOpen"
              :disabled="!editor || !uploadImage"
              title="Insertar imagen"
              @mousedown.prevent="openInsertImageDialog"
              @click="openInsertImageDialog"
            >
              <span class="editor-label-sr">Insertar imagen</span>
              <span aria-hidden="true">Imagen</span>
            </button>
            <button
              type="button"
              class="editor-button editor-button-icon"
              :disabled="!editor"
              title="Deshacer"
              @click="editor?.chain().focus().undo().run()"
            >
              <span class="editor-label-sr">Deshacer</span>
              <span aria-hidden="true">↶</span>
            </button>
            <button
              type="button"
              class="editor-button editor-button-icon"
              :disabled="!editor"
              title="Rehacer"
              @click="editor?.chain().focus().redo().run()"
            >
              <span class="editor-label-sr">Rehacer</span>
              <span aria-hidden="true">↷</span>
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

      <div
        v-if="isImageDialogOpen"
        class="grid gap-4 rounded-2xl border border-emerald-100 bg-white p-4"
        role="dialog"
        aria-modal="false"
        aria-labelledby="post-content-image-title"
      >
        <h3 id="post-content-image-title" class="text-base font-black text-slate-950">
          {{ imageDialogMode === 'insert' ? 'Insertar imagen' : 'Editar imagen' }}
        </h3>

        <div v-if="imageDialogMode === 'insert'">
          <label class="editor-label" for="post-content-image-file">Archivo</label>
          <input
            id="post-content-image-file"
            ref="imageFileInput"
            type="file"
            accept="image/jpeg,image/png"
            class="mt-2 w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm font-bold text-slate-950 outline-none focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
            @change="selectImageFile"
          />
        </div>

        <div>
          <label class="editor-label" for="post-content-image-alt">Texto alternativo</label>
          <input
            id="post-content-image-alt"
            ref="imageAltInput"
            v-model="imageAltText"
            type="text"
            maxlength="180"
            class="mt-2 w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm font-bold text-slate-950 outline-none focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
            placeholder="Describe la imagen para accesibilidad"
          />
        </div>

        <div>
          <label class="editor-label" for="post-content-image-caption">Pie de imagen</label>
          <textarea
            id="post-content-image-caption"
            v-model="imageCaption"
            maxlength="240"
            rows="3"
            class="mt-2 w-full resize-y rounded-2xl border border-slate-300 px-4 py-3 text-sm font-bold text-slate-950 outline-none focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
            placeholder="Texto opcional para acompañar la figura"
          />
          <p class="mt-2 text-xs font-bold text-slate-600">
            {{ imageCaption.trim().length }}/240
          </p>
        </div>

        <div>
          <label class="editor-label" for="post-content-image-display-size">Tamaño visual</label>
          <select
            id="post-content-image-display-size"
            v-model="imageDisplaySize"
            class="mt-2 w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm font-bold text-slate-950 outline-none focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
          >
            <option value="small">Pequeña</option>
            <option value="medium">Mediana</option>
            <option value="large">Grande</option>
          </select>
        </div>

        <p v-if="imageError" class="rounded-2xl bg-red-50 px-4 py-3 text-sm font-bold text-red-900" role="alert">
          {{ imageError }}
        </p>

        <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            class="editor-button"
            :disabled="isImageSubmitting"
            @click="closeImageDialog"
          >
            Cancelar
          </button>
          <button
            type="button"
            class="editor-button editor-button-primary"
            :disabled="isImageSubmitting"
            @click="submitImageDialog"
          >
            {{ isImageSubmitting ? 'Procesando...' : imageDialogMode === 'insert' ? 'Insertar imagen' : 'Guardar imagen' }}
          </button>
        </div>
      </div>
    </div>

    <div class="relative min-w-0 max-w-full">
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
  display: contents;
  min-width: 0;
  max-width: 100%;
}

.editor-toolbar {
  position: sticky;
  top: 1rem;
  z-index: 20;
  min-width: 0;
  max-width: 100%;
  box-shadow: 0 12px 26px rgb(15 23 42 / 0.08);
}

.editor-group {
  display: grid;
  gap: 0.2rem;
  min-width: 0;
  max-width: 100%;
}

.editor-toolbar > .editor-group > .editor-label,
.editor-toolbar-row > .editor-group > .editor-label {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.editor-label-sr {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.editor-label {
  font-size: 0.64rem;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
  color: rgb(4 120 87);
}

.editor-control-set {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  min-width: 0;
  max-width: 100%;
}

.editor-menu {
  position: relative;
  align-self: end;
  min-width: 0;
}

.editor-menu > summary {
  list-style: none;
}

.editor-menu > summary::-webkit-details-marker {
  display: none;
}

.editor-menu-trigger {
  display: inline-flex;
  min-height: 2.1rem;
  cursor: pointer;
  align-items: center;
  border-radius: 9999px;
  border: 1px solid rgb(203 213 225);
  background: white;
  padding: 0.42rem 0.72rem;
  font-size: 0.75rem;
  font-weight: 900;
  color: rgb(15 23 42);
  transition:
    background-color 150ms ease,
    border-color 150ms ease,
    box-shadow 150ms ease,
    color 150ms ease;
}

.editor-menu-trigger::after {
  content: '▾';
  margin-left: 0.35rem;
  font-size: 0.68rem;
}

.editor-menu[open] .editor-menu-trigger,
.editor-menu-trigger:hover {
  border-color: rgb(4 120 87);
  background: rgb(4 120 87);
  color: white;
}

.editor-menu-panel {
  position: absolute;
  top: calc(100% + 0.45rem);
  left: 0;
  z-index: 30;
  display: grid;
  width: min(18rem, calc(100vw - 2rem));
  gap: 0.7rem;
  border-radius: 1rem;
  border: 1px solid rgb(226 232 240);
  background: white;
  padding: 0.8rem;
  box-shadow: 0 18px 42px rgb(15 23 42 / 0.16);
}

.editor-select,
.editor-button {
  min-height: 2.1rem;
  border-radius: 9999px;
  border: 1px solid rgb(203 213 225);
  background: white;
  padding: 0.42rem 0.68rem;
  font-size: 0.75rem;
  font-weight: 900;
  color: rgb(15 23 42);
  max-width: 100%;
  transition:
    background-color 150ms ease,
    border-color 150ms ease,
    box-shadow 150ms ease,
    color 150ms ease;
}

.editor-select {
  min-width: 7.5rem;
  padding-right: 1.7rem;
}

.editor-button {
  white-space: normal;
  text-align: center;
}

.editor-button:has(.editor-label-sr) {
  min-width: 2.1rem;
  padding-inline: 0.62rem;
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
.editor-select:focus-visible,
.editor-menu-trigger:focus-visible {
  outline: 2px solid rgb(4 120 87);
  outline-offset: 3px;
}

:deep(.ProseMirror) {
  min-height: 26rem;
  max-width: 100%;
  border-radius: 0 0 1.5rem 1.5rem;
  overflow-wrap: anywhere;
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

:deep(.editor-image-node) {
  position: relative;
  margin: 1.5rem auto;
  width: fit-content;
  max-width: min(100%, 40rem);
  border-radius: 1.5rem;
  border: 1px solid rgb(203 213 225);
  background: rgb(248 250 252);
  padding: 0.75rem;
}

:deep(.editor-image-node[data-display-size='small']) {
  max-width: min(100%, 22.5rem);
}

:deep(.editor-image-node[data-display-size='large']) {
  width: 100%;
  max-width: 100%;
}

:deep(.editor-image-node-selected) {
  border-color: rgb(4 120 87);
  box-shadow: 0 0 0 4px rgb(167 243 208 / 0.9);
}

:deep(.editor-image-frame) {
  overflow: hidden;
  border-radius: 1rem;
  background: white;
}

:deep(.editor-image-frame img) {
  display: block;
  width: auto;
  max-width: 100%;
  height: auto;
}

:deep(.editor-image-node[data-display-size='large'] .editor-image-frame img) {
  width: 100%;
}

:deep(.editor-image-caption) {
  margin-top: 0.7rem;
  text-align: center;
  font-size: 0.92rem;
  font-weight: 700;
  line-height: 1.6;
  color: rgb(71 85 105);
}

:deep(.editor-image-missing) {
  border-radius: 1rem;
  background: rgb(241 245 249);
  padding: 2rem;
  text-align: center;
  font-weight: 900;
  color: rgb(71 85 105);
}

:deep(.editor-image-edit-button) {
  margin-top: 0.75rem;
  border-radius: 9999px;
  background: rgb(4 120 87);
  padding: 0.45rem 0.85rem;
  font-size: 0.78rem;
  font-weight: 900;
  color: white;
}

:deep(.editor-image-edit-button:focus-visible) {
  outline: 2px solid rgb(4 120 87);
  outline-offset: 3px;
}
</style>
