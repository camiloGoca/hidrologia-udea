<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { isAxiosError } from 'axios'

import AcademicPostEditor from '@/components/AcademicPostEditor.vue'
import PostContentRenderer from '@/components/PostContentRenderer'
import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import {
  archiveAdminPost,
  deleteAdminPost,
  deleteAdminPostImage,
  discardManualAdminPost,
  getAdminPost,
  publishAdminPost,
  restoreAdminPost,
  updateAdminPostImageAltText,
  updateAdminPost,
  uploadAdminPostImage,
} from '@/services/api/adminPostService'
import { getAdminTags } from '@/services/api/adminTagService'
import { discardQuestionDraft } from '@/services/api/adminService'
import { getSections } from '@/services/api/sectionService'
import { signOut } from '@/services/firebase/authService'
import type { AdminTag } from '@/types/adminTag'
import type { AdminPost, AdminPostImage } from '@/types/adminPost'
import {
  emptyPostContentDocument,
  type PostContentDocument,
  type PostContentNode,
} from '@/types/postContent'
import type { Section, SectionType } from '@/types/section'
import { adminPostStatusLabel } from '@/utils/adminPostStatus'
import { extractPostContentText, samePostContent } from '@/utils/postContent'

type ConfirmationAction = 'discard' | 'publish' | 'archive' | 'restore' | 'deletePermanent'
interface PostSaveSnapshot {
  title: string
  contentDocument: PostContentDocument
  sectionSlug: string
  tagIds: number[]
}

const AUTOSAVE_DELAY_MS = 1500

const route = useRoute()
const router = useRouter()
const post = ref<AdminPost | null>(null)
const sections = ref<Section[]>([])
const availableTags = ref<AdminTag[]>([])
const isLoading = ref(true)
const hasError = ref(false)
const pendingAction = ref<ConfirmationAction | null>(null)
const isSubmittingAction = ref(false)
const isSaving = ref(false)
const isAutosavePending = ref(false)
const deletingUnusedImageId = ref<number | null>(null)
const actionError = ref(false)
const saveError = ref(false)
const unusedImageError = ref('')
const unusedImageSuccess = ref('')
const successMessage = ref('')
const isPreviewOpen = ref(false)
const cancelButton = ref<HTMLButtonElement | null>(null)
const previewCloseButton = ref<HTMLButtonElement | null>(null)
let lastFocusedElement: HTMLElement | null = null
let autosaveTimer: ReturnType<typeof setTimeout> | null = null
let isSyncingForm = false
const form = reactive({
  title: '',
  contentDocument: emptyPostContentDocument(),
  sectionSlug: '',
  tagIds: [] as number[],
})
const saved = reactive({
  title: '',
  contentDocument: emptyPostContentDocument(),
  sectionSlug: '',
  tagIds: [] as number[],
})

const sourceQuestion = computed(() => post.value?.sourceQuestion ?? null)
const referencedImageIds = computed(() => collectReferencedImageIds(form.contentDocument))
const unusedImages = computed(() =>
  post.value?.images.filter((image) => !referencedImageIds.value.has(image.id)) ?? [],
)
const displayNickname = computed(() => sourceQuestion.value?.nickname ?? 'Anónimo')
const displayTitle = computed(() => form.title.trim() || 'Sin título')
const workshopSections = computed(() => sectionsByType('TALLER'))
const examSections = computed(() => sectionsByType('PARCIAL'))
const selectedSection = computed(
  () => sections.value.find((section) => section.slug === form.sectionSlug) ?? post.value?.section ?? null,
)
const selectedTags = computed(() =>
  availableTags.value.filter((tag) => form.tagIds.includes(tag.id)),
)
const isDraft = computed(() => post.value?.status === 'DRAFT')
const isPublished = computed(() => post.value?.status === 'PUBLISHED')
const isArchived = computed(() => post.value?.status === 'ARCHIVED')
const isAutosaveEnabled = computed(() => isDraft.value || isArchived.value)
const isBusy = computed(() => isSaving.value || isSubmittingAction.value || deletingUnusedImageId.value !== null)
const isDirty = computed(
  () =>
    form.title !== saved.title ||
    !samePostContent(form.contentDocument, saved.contentDocument) ||
    form.sectionSlug !== saved.sectionSlug ||
    !sameIds(form.tagIds, saved.tagIds),
)
const hasRequiredPublishedContent = computed(
  () => form.title.trim().length > 0 && extractPostContentText(form.contentDocument).length > 0,
)
const canAutosaveCurrentForm = computed(
  () =>
    Boolean(post.value) &&
    isAutosaveEnabled.value &&
    isDirty.value &&
    (isDraft.value || hasRequiredPublishedContent.value),
)
const canSave = computed(() => {
  if (!post.value || !isPublished.value || !isDirty.value || isBusy.value) {
    return false
  }

  return hasRequiredPublishedContent.value
})
const canPublish = computed(
  () => isDraft.value && !isDirty.value && hasRequiredPublishedContent.value && !isBusy.value,
)
const canArchive = computed(() => isPublished.value && !isDirty.value && !isBusy.value)
const canRestore = computed(
  () => isArchived.value && !isDirty.value && hasRequiredPublishedContent.value && !isBusy.value,
)
const saveButtonLabel = computed(() => (isDraft.value ? 'Guardar borrador' : 'Guardar cambios'))
const savingLabel = computed(() => (isDraft.value ? 'Guardando...' : 'Actualizando...'))
const autosaveStatusMessage = computed(() => {
  if (!isAutosaveEnabled.value) {
    return ''
  }
  if (successMessage.value) {
    return ''
  }
  if (isSaving.value) {
    return 'Guardando...'
  }
  if (saveError.value) {
    return 'No se pudo guardar'
  }
  if (isDirty.value || isAutosavePending.value) {
    return 'Cambios sin guardar'
  }

  return 'Guardado'
})
const statusHelpMessage = computed(() => {
  if (isDirty.value) {
    if (isPublished.value) {
      return 'Guarda los cambios antes de archivar.'
    }

    if (isArchived.value) {
      return 'Guarda los cambios antes de restaurar.'
    }

    return 'Guarda los cambios antes de publicar.'
  }

  if (!isDraft.value && !hasRequiredPublishedContent.value) {
    return 'Título y contenido son obligatorios para publicaciones publicadas o archivadas.'
  }

  return ''
})
const confirmationConfig = computed(() => {
  switch (pendingAction.value) {
    case 'discard':
      return {
        title: '¿Descartar este borrador?',
        description: sourceQuestion.value
          ? 'El borrador se eliminará. La pregunta original y su imagen permanecerán intactas y seguirán pendientes.'
          : 'El borrador se eliminará. No hay una pregunta de origen asociada a esta publicación.',
        confirmLabel: 'Descartar borrador',
        confirmClass: 'bg-red-700 hover:bg-red-800 focus-visible:outline-red-800',
      }
    case 'publish':
      return {
        title: '¿Publicar esta publicación?',
        description: sourceQuestion.value
          ? 'Será visible para los estudiantes y la pregunta de origen quedará marcada como publicada.'
          : 'Será visible para los estudiantes. Esta publicación no tiene pregunta de origen.',
        confirmLabel: 'Publicar',
        confirmClass: 'bg-emerald-700 hover:bg-emerald-800 focus-visible:outline-emerald-800',
      }
    case 'archive':
      return {
        title: '¿Archivar esta publicación?',
        description:
          'La publicación dejará de ser visible para los estudiantes, pero se conservará en el panel administrativo.',
        confirmLabel: 'Archivar publicación',
        confirmClass: 'bg-red-700 hover:bg-red-800 focus-visible:outline-red-800',
      }
    case 'restore':
      return {
        title: '¿Restaurar esta publicación?',
        description: 'Volverá a ser visible para los estudiantes.',
        confirmLabel: 'Restaurar publicación',
        confirmClass: 'bg-emerald-700 hover:bg-emerald-800 focus-visible:outline-emerald-800',
      }
    case 'deletePermanent':
      return {
        title: sourceQuestion.value
          ? '¿Eliminar esta publicación y su pregunta original?'
          : '¿Eliminar esta publicación definitivamente?',
        description: sourceQuestion.value
          ? 'Esta publicación proviene de una pregunta de estudiante. Se eliminarán definitivamente la publicación, sus imágenes, la pregunta original y su imagen adjunta. Esta acción no se puede deshacer.'
          : 'Se eliminarán también sus imágenes y sus datos de visualización. Esta acción no se puede deshacer.',
        confirmLabel: 'Eliminar definitivamente',
        confirmClass: 'bg-red-800 hover:bg-red-900 focus-visible:outline-red-900',
      }
    default:
      return null
  }
})

void loadPost()

watch(
  () => [
    form.title,
    JSON.stringify(form.contentDocument),
    form.sectionSlug,
    JSON.stringify(sortedIds(form.tagIds)),
    post.value?.status,
  ],
  () => {
    if (!isSyncingForm) {
      scheduleAutosave()
    }
  },
)

onBeforeUnmount(() => {
  clearAutosaveTimer()
})

async function loadPost() {
  const id = Number(route.params.id)

  if (!Number.isFinite(id)) {
    isLoading.value = false
    hasError.value = true
    return
  }

  isLoading.value = true
  hasError.value = false

  try {
    const [postResponse, sectionResponse, tagResponse] = await Promise.all([
      getAdminPost(id),
      getSections(),
      getAdminTags(),
    ])
    post.value = postResponse
    sections.value = sectionResponse
    availableTags.value = tagResponse
    syncForm(postResponse)
  } catch (error) {
    await handleError(error, () => {
      hasError.value = true
      post.value = null
    })
  } finally {
    isLoading.value = false
  }
}

async function savePost() {
  if (!post.value || !canSave.value) {
    return
  }

  clearAutosaveTimer()
  await persistSnapshot(snapshotForm(), 'Publicación actualizada.')
}

async function persistSnapshot(snapshot: PostSaveSnapshot, message: string) {
  if (!post.value || isSaving.value) {
    return
  }

  isSaving.value = true
  isAutosavePending.value = false
  saveError.value = false
  successMessage.value = ''

  try {
    const updatedPost = await updateAdminPost(post.value.id, {
      title: snapshot.title,
      contentDocument: snapshot.contentDocument,
      sectionSlug: snapshot.sectionSlug,
      tagIds: [...snapshot.tagIds],
    })
    if (sameSnapshot(snapshotForm(), snapshot)) {
      post.value = updatedPost
      syncForm(updatedPost)
      successMessage.value = message
    } else {
      post.value = updatedPost
      syncSaved(updatedPost)
    }
  } catch (error) {
    await handleError(error, () => {
      saveError.value = true
    })
  } finally {
    isSaving.value = false
    if (isAutosaveEnabled.value && isDirty.value && !saveError.value) {
      scheduleAutosave()
    }
  }
}

function scheduleAutosave() {
  if (!isAutosaveEnabled.value) {
    clearAutosaveTimer()
    return
  }

  if (!isDirty.value) {
    clearAutosaveTimer()
    saveError.value = false
    return
  }

  if (!canAutosaveCurrentForm.value) {
    clearAutosaveTimer()
    return
  }

  clearAutosaveTimer()
  successMessage.value = ''
  saveError.value = false
  isAutosavePending.value = true
  autosaveTimer = setTimeout(() => {
    void runAutosave()
  }, AUTOSAVE_DELAY_MS)
}

async function runAutosave() {
  if (!canAutosaveCurrentForm.value) {
    isAutosavePending.value = false
    return
  }

  if (isSaving.value) {
    scheduleAutosave()
    return
  }

  await persistSnapshot(snapshotForm(), 'Guardado')
}

async function retryAutosave() {
  if (!canAutosaveCurrentForm.value) {
    return
  }

  clearAutosaveTimer()
  saveError.value = false
  await runAutosave()
}

async function uploadEditorImage(file: File, altText: string): Promise<AdminPostImage> {
  if (!post.value) {
    throw new Error('Post is not loaded')
  }

  const image = await uploadAdminPostImage(post.value.id, { file, altText })
  post.value = {
    ...post.value,
    images: upsertImage(post.value.images, image),
  }

  return image
}

async function updateEditorImageAltText(imageId: number, altText: string): Promise<AdminPostImage> {
  if (!post.value) {
    throw new Error('Post is not loaded')
  }

  const image = await updateAdminPostImageAltText(post.value.id, imageId, altText)
  post.value = {
    ...post.value,
    images: upsertImage(post.value.images, image),
  }

  return image
}

async function deleteEditorImage(imageId: number): Promise<void> {
  if (!post.value) {
    throw new Error('Post is not loaded')
  }

  await deleteAdminPostImage(post.value.id, imageId)
  post.value = {
    ...post.value,
    images: post.value.images.filter((image) => image.id !== imageId),
  }
}

async function deleteUnusedImage(imageId: number): Promise<void> {
  if (!post.value || deletingUnusedImageId.value !== null) {
    return
  }

  deletingUnusedImageId.value = imageId
  unusedImageError.value = ''
  unusedImageSuccess.value = ''

  try {
    await deleteAdminPostImage(post.value.id, imageId)
    post.value = {
      ...post.value,
      images: post.value.images.filter((image) => image.id !== imageId),
    }
    unusedImageSuccess.value = 'Archivo eliminado.'
  } catch (error) {
    await handleError(error, () => {
      unusedImageError.value = unusedImageDeleteMessage(error)
    })
  } finally {
    deletingUnusedImageId.value = null
  }
}

async function openConfirmation(action: ConfirmationAction) {
  actionError.value = false
  pendingAction.value = action
  lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null
  await nextTick()
  cancelButton.value?.focus()
}

async function openPreview() {
  lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null
  isPreviewOpen.value = true
  await nextTick()
  previewCloseButton.value?.focus()
}

function closePreview() {
  isPreviewOpen.value = false
  lastFocusedElement?.focus()
}

function closeConfirmation() {
  if (isSubmittingAction.value) {
    return
  }

  pendingAction.value = null
  lastFocusedElement?.focus()
}

async function confirmAction() {
  if (!post.value || !pendingAction.value || isSubmittingAction.value) {
    return
  }

  if (!canRunAction(pendingAction.value)) {
    return
  }

  isSubmittingAction.value = true
  actionError.value = false
  successMessage.value = ''

  try {
    if (pendingAction.value === 'discard') {
      if (sourceQuestion.value) {
        await discardQuestionDraft(sourceQuestion.value.id)
        await router.push({
          name: 'admin-question-detail',
          params: { id: sourceQuestion.value.id },
        })
        return
      }

      await discardManualAdminPost(post.value.id)
      await router.push({
        name: 'admin-posts',
        query: { estado: 'borradores' },
      })
      return
    }

    if (pendingAction.value === 'deletePermanent') {
      await deleteAdminPost(post.value.id)
      pendingAction.value = null
      await router.push({
        name: 'admin-posts',
        query: { estado: 'archivadas' },
      })
      return
    }

    const updatedPost = await runPostAction(pendingAction.value, post.value.id)
    post.value = updatedPost
    syncForm(updatedPost)
    successMessage.value = successMessageFor(pendingAction.value)
    pendingAction.value = null
  } catch (error) {
    await handleError(error, () => {
      actionError.value = true
    })
  } finally {
    isSubmittingAction.value = false
  }
}

function canRunAction(action: ConfirmationAction): boolean {
  switch (action) {
    case 'discard':
      return isDraft.value && !isBusy.value
    case 'publish':
      return canPublish.value
    case 'archive':
      return canArchive.value
    case 'restore':
      return canRestore.value
    case 'deletePermanent':
      return isArchived.value && !isBusy.value
  }
}

function runPostAction(action: Exclude<ConfirmationAction, 'discard' | 'deletePermanent'>, id: number) {
  switch (action) {
    case 'publish':
      return publishAdminPost(id)
    case 'archive':
      return archiveAdminPost(id)
    case 'restore':
      return restoreAdminPost(id)
  }
}

function successMessageFor(action: ConfirmationAction): string {
  switch (action) {
    case 'publish':
      return 'Publicación publicada.'
    case 'archive':
      return 'Publicación archivada.'
    case 'restore':
      return 'Publicación restaurada.'
    case 'deletePermanent':
      return ''
    case 'discard':
      return ''
  }
}

async function handleError(error: unknown, fallback: () => void) {
  if (isAdminAuthorizationError(error)) {
    await signOut().catch(() => undefined)
    await router.push({ name: 'admin-login', query: { reason: 'forbidden' } })
    return
  }

  fallback()
}

function syncForm(currentPost: AdminPost) {
  isSyncingForm = true
  try {
    form.title = currentPost.title
    form.contentDocument = cloneContent(currentPost.contentDocument)
    form.sectionSlug = currentPost.section.slug
    form.tagIds = sortedIds(currentPost.tags.map((tag) => tag.id))
    syncSaved(currentPost)
    clearAutosaveTimer()
    saveError.value = false
    isAutosavePending.value = false
  } finally {
    isSyncingForm = false
  }
}

function syncSaved(currentPost: AdminPost) {
  saved.title = currentPost.title
  saved.contentDocument = cloneContent(currentPost.contentDocument)
  saved.sectionSlug = currentPost.section.slug
  saved.tagIds = sortedIds(currentPost.tags.map((tag) => tag.id))
}

function snapshotForm(): PostSaveSnapshot {
  return {
    title: form.title,
    contentDocument: cloneContent(form.contentDocument),
    sectionSlug: form.sectionSlug,
    tagIds: sortedIds(form.tagIds),
  }
}

function sameSnapshot(left: PostSaveSnapshot, right: PostSaveSnapshot) {
  return (
    left.title === right.title &&
    samePostContent(left.contentDocument, right.contentDocument) &&
    left.sectionSlug === right.sectionSlug &&
    sameIds(left.tagIds, right.tagIds)
  )
}

function clearAutosaveTimer() {
  if (autosaveTimer) {
    clearTimeout(autosaveTimer)
    autosaveTimer = null
  }
  isAutosavePending.value = false
}

function upsertImage(images: AdminPostImage[], image: AdminPostImage) {
  const withoutImage = images.filter((currentImage) => currentImage.id !== image.id)

  return [...withoutImage, image].sort((left, right) => left.id - right.id)
}

function cloneContent(document: PostContentDocument) {
  return JSON.parse(JSON.stringify(document ?? emptyPostContentDocument())) as PostContentDocument
}

function sectionsByType(type: SectionType) {
  return sections.value.filter((section) => section.type === type)
}

function sortedIds(ids: number[]) {
  return [...new Set(ids)].sort((left, right) => left - right)
}

function sameIds(left: number[], right: number[]) {
  const sortedLeft = sortedIds(left)
  const sortedRight = sortedIds(right)

  return (
    sortedLeft.length === sortedRight.length &&
    sortedLeft.every((id, index) => id === sortedRight[index])
  )
}

function collectReferencedImageIds(document: PostContentDocument): Set<number> {
  const ids = new Set<number>()
  collectReferencedImageIdsFromNodes(document.content ?? [], ids)

  return ids
}

function collectReferencedImageIdsFromNodes(nodes: PostContentNode[], ids: Set<number>) {
  for (const node of nodes) {
    if (node.type === 'image' && typeof node.attrs?.postImageId === 'number') {
      ids.add(node.attrs.postImageId)
    }

    if (node.content?.length) {
      collectReferencedImageIdsFromNodes(node.content, ids)
    }
  }
}

function unusedImageDeleteMessage(error: unknown): string {
  if (isAxiosError(error) && error.response?.status === 409) {
    return 'Guarda primero el documento sin esta imagen antes de eliminar el archivo.'
  }

  return 'No pudimos eliminar esta imagen no utilizada. Intenta nuevamente.'
}
</script>

<template>
  <main class="px-5 py-10 sm:px-6">
    <section class="mx-auto max-w-5xl">
      <RouterLink
        :to="{ name: 'admin-posts' }"
        class="rounded-md text-sm font-black text-sky-800 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
      >
        Volver a publicaciones
      </RouterLink>

      <div
        v-if="isLoading"
        class="mt-8 rounded-3xl border border-sky-100 bg-sky-50 px-6 py-10 text-center font-bold text-sky-950"
        role="status"
      >
        Cargando publicación...
      </div>

      <div
        v-else-if="hasError || !post"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
        role="alert"
      >
        <p class="text-lg font-black text-red-950">No pudimos cargar esta publicación.</p>
        <p class="mt-2 text-sm leading-6 text-red-800">
          Puede que no exista o que no esté disponible para tu sesión.
        </p>
      </div>

      <article v-else class="mt-8 rounded-[2rem] bg-white shadow-sm ring-1 ring-slate-200">
        <header class="border-b border-slate-200 p-6 sm:p-8">
          <div class="flex flex-wrap items-center gap-2 text-xs font-black uppercase">
            <span class="rounded-full bg-sky-100 px-3 py-1 text-sky-950">
              {{ adminPostStatusLabel(post.status) }}
            </span>
            <span class="rounded-full bg-emerald-100 px-3 py-1 text-emerald-900">
              {{ post.section.name }}
            </span>
          </div>

          <div class="mt-5 flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
            <div>
              <p class="text-sm font-black uppercase text-emerald-700">
                Editor de publicación
              </p>
              <h1 class="mt-2 text-3xl font-black sm:text-4xl">{{ displayTitle }}</h1>
            </div>

            <div class="flex flex-wrap gap-3">
              <button
                type="button"
                class="inline-flex rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950"
                @click="openPreview"
              >
                Vista previa
              </button>
              <RouterLink
                v-if="isPublished"
                :to="{ name: 'post-detail', params: { id: post.id } }"
                class="inline-flex rounded-2xl bg-emerald-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
              >
                Ver publicación pública
              </RouterLink>
              <button
                v-if="isDraft"
                type="button"
                class="rounded-2xl bg-red-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-red-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                :disabled="isBusy"
                @click="openConfirmation('discard')"
              >
                Descartar borrador
              </button>
              <button
                v-if="isPublished"
                type="button"
                class="rounded-2xl bg-red-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-red-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                :disabled="!canArchive"
                @click="openConfirmation('archive')"
              >
                Archivar publicación
              </button>
              <button
                v-if="isArchived"
                type="button"
                class="rounded-2xl bg-emerald-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                :disabled="!canRestore"
                @click="openConfirmation('restore')"
              >
                Restaurar publicación
              </button>
              <button
                v-if="isArchived"
                type="button"
                class="rounded-2xl bg-red-800 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-red-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-900 disabled:cursor-not-allowed disabled:bg-slate-400"
                :disabled="isBusy"
                @click="openConfirmation('deletePermanent')"
              >
                Eliminar definitivamente
              </button>
            </div>
          </div>

          <p
            v-if="isPublished"
            class="mt-5 rounded-2xl bg-amber-50 px-4 py-3 text-sm font-bold text-amber-950"
          >
            Los cambios guardados se reflejarán inmediatamente en la publicación pública.
          </p>

          <div
            v-if="autosaveStatusMessage"
            class="mt-5 flex flex-wrap items-center gap-3 rounded-2xl bg-slate-50 px-4 py-3 text-sm font-bold text-slate-700"
            role="status"
          >
            <span>{{ autosaveStatusMessage }}</span>
            <button
              v-if="saveError"
              type="button"
              class="rounded-full bg-sky-950 px-4 py-2 text-xs font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950 disabled:cursor-not-allowed disabled:bg-slate-400"
              :disabled="isBusy || !canAutosaveCurrentForm"
              @click="retryAutosave"
            >
              Reintentar
            </button>
          </div>
          <p
            v-else-if="isDirty"
            class="mt-5 rounded-2xl bg-amber-50 px-4 py-3 text-sm font-bold text-amber-950"
            role="status"
          >
            Cambios sin guardar
          </p>
          <p
            v-else-if="successMessage"
            class="mt-5 rounded-2xl bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-900"
            role="status"
          >
            {{ successMessage }}
          </p>
          <p
            v-if="statusHelpMessage"
            id="post-action-help"
            class="mt-5 rounded-2xl bg-amber-50 px-4 py-3 text-sm font-bold text-amber-950"
          >
            {{ statusHelpMessage }}
          </p>
          <p
            v-if="saveError"
            class="mt-5 rounded-2xl bg-red-50 px-4 py-3 text-sm font-bold text-red-900"
            role="alert"
          >
            No pudimos guardar los cambios. Revisa que tenga título y contenido cuando corresponda.
          </p>
          <p
            v-if="actionError"
            class="mt-5 rounded-2xl bg-red-50 px-4 py-3 text-sm font-bold text-red-900"
            role="alert"
          >
            No pudimos completar la acción. Intenta nuevamente en unos momentos.
          </p>
        </header>

        <div class="grid gap-8 p-6 sm:p-8">
          <form class="grid gap-6" @submit.prevent="savePost">
            <div>
              <label for="post-title" class="text-sm font-black uppercase text-emerald-700">
                Título
              </label>
              <input
                id="post-title"
                v-model="form.title"
                type="text"
                maxlength="180"
                class="mt-3 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
              />
            </div>

            <div>
              <label for="post-section" class="text-sm font-black uppercase text-emerald-700">
                Sección
              </label>
              <select
                id="post-section"
                v-model="form.sectionSlug"
                class="mt-3 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
              >
                <optgroup v-if="workshopSections.length" label="Talleres">
                  <option
                    v-for="section in workshopSections"
                    :key="section.slug"
                    :value="section.slug"
                  >
                    {{ section.name }}
                  </option>
                </optgroup>
                <optgroup v-if="examSections.length" label="Parciales">
                  <option
                    v-for="section in examSections"
                    :key="section.slug"
                    :value="section.slug"
                  >
                    {{ section.name }}
                  </option>
                </optgroup>
              </select>
            </div>

            <div>
              <label for="post-content" class="text-sm font-black uppercase text-emerald-700">
                Contenido
              </label>
              <AcademicPostEditor
                id="post-content"
                v-model="form.contentDocument"
                :images="post.images"
                :upload-image="uploadEditorImage"
                :update-image-alt-text="updateEditorImageAltText"
                :delete-image="deleteEditorImage"
              />
            </div>

            <section
              v-if="unusedImages.length || unusedImageError || unusedImageSuccess"
              aria-labelledby="unused-images-title"
              class="rounded-3xl border border-amber-200 bg-amber-50 p-5"
            >
              <h2 id="unused-images-title" class="text-sm font-black uppercase text-amber-950">
                Imágenes no utilizadas
              </h2>
              <p class="mt-2 text-sm leading-6 text-amber-950/80">
                Estas imágenes fueron subidas pero ya no aparecen en la publicación.
              </p>

              <p
                v-if="unusedImageSuccess"
                class="mt-4 rounded-2xl bg-white px-4 py-3 text-sm font-bold text-emerald-900"
                role="status"
              >
                {{ unusedImageSuccess }}
              </p>
              <p
                v-if="unusedImageError"
                class="mt-4 rounded-2xl bg-white px-4 py-3 text-sm font-bold text-red-900"
                role="alert"
              >
                {{ unusedImageError }}
              </p>

              <ul v-if="unusedImages.length" class="mt-4 grid gap-3">
                <li
                  v-for="image in unusedImages"
                  :key="image.id"
                  class="flex min-w-0 max-w-full flex-col gap-4 rounded-2xl border border-amber-200 bg-white p-3 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div class="flex min-w-0 items-center gap-3">
                    <img
                      :src="image.secureUrl"
                      :alt="image.altText"
                      class="size-16 shrink-0 rounded-xl object-cover"
                    />
                    <div class="min-w-0">
                      <p class="break-words text-sm font-black text-slate-950">
                        {{ image.altText }}
                      </p>
                      <p class="mt-1 text-xs font-bold uppercase text-slate-500">
                        {{ image.format }} · {{ image.width }}x{{ image.height }}
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    class="w-full rounded-2xl bg-red-700 px-4 py-2 text-sm font-black text-white shadow-sm transition hover:bg-red-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-800 disabled:cursor-not-allowed disabled:bg-slate-400 sm:w-auto"
                    :disabled="isBusy"
                    @click="deleteUnusedImage(image.id)"
                  >
                    {{ deletingUnusedImageId === image.id ? 'Eliminando...' : 'Eliminar archivo' }}
                  </button>
                </li>
              </ul>
            </section>

            <fieldset class="rounded-3xl border border-slate-200 bg-slate-50 p-5">
              <legend class="px-2 text-sm font-black uppercase text-emerald-700">
                Hashtags
              </legend>
              <p class="mt-2 text-sm leading-6 text-slate-600">
                Los hashtags se guardan junto con el contenido y la sección de la publicación.
              </p>

              <div v-if="availableTags.length" class="mt-4 flex flex-wrap gap-3">
                <label
                  v-for="tag in availableTags"
                  :key="tag.id"
                  class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-black text-slate-700 transition hover:border-emerald-300 hover:bg-emerald-50 has-[:checked]:border-emerald-700 has-[:checked]:bg-emerald-700 has-[:checked]:text-white"
                >
                  <input
                    v-model="form.tagIds"
                    type="checkbox"
                    class="size-4 accent-emerald-700"
                    :value="tag.id"
                  />
                  #{{ tag.name }}
                </label>
              </div>

              <p v-else class="mt-4 rounded-2xl bg-white px-4 py-3 text-sm font-bold text-slate-600">
                No hay hashtags creados todavía.
                <RouterLink
                  :to="{ name: 'admin-hashtags' }"
                  class="text-emerald-800 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
                >
                  Crear hashtags
                </RouterLink>
              </p>
            </fieldset>

            <div class="flex flex-col gap-3 sm:flex-row sm:justify-end">
              <button
                type="button"
                class="rounded-2xl bg-white px-5 py-3 text-sm font-black text-sky-950 shadow-sm ring-1 ring-slate-200 transition hover:bg-sky-50 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950"
                @click="openPreview"
              >
                Vista previa
              </button>
              <button
                v-if="isPublished"
                type="submit"
                class="rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950 disabled:cursor-not-allowed disabled:bg-slate-400"
                :disabled="!canSave"
              >
                {{ isSaving ? savingLabel : saveButtonLabel }}
              </button>
              <button
                v-if="isDraft"
                type="button"
                class="rounded-2xl bg-emerald-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                :aria-describedby="statusHelpMessage ? 'post-action-help' : undefined"
                :disabled="!canPublish"
                @click="openConfirmation('publish')"
              >
                Publicar
              </button>
            </div>
          </form>

          <section v-if="sourceQuestion" aria-labelledby="source-question-title">
            <h2 id="source-question-title" class="text-sm font-black uppercase text-emerald-700">
              Pregunta de origen
            </h2>
            <div class="mt-4 rounded-3xl border border-slate-200 bg-slate-50 p-5">
              <p class="text-sm font-bold text-slate-500">
                {{ displayNickname }}
              </p>
              <p class="mt-3 whitespace-pre-wrap text-lg leading-8 text-slate-800">
                {{ sourceQuestion.question }}
              </p>
              <p
                v-if="sourceQuestion.hasAttachment"
                class="mt-4 rounded-2xl bg-cyan-100 px-4 py-3 text-sm font-black text-cyan-950"
              >
                La pregunta original tiene una imagen adjunta privada.
              </p>
              <RouterLink
                :to="{ name: 'admin-question-detail', params: { id: sourceQuestion.id } }"
                class="mt-5 inline-flex rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950"
              >
                Ver pregunta original
              </RouterLink>
            </div>
          </section>
        </div>
      </article>
    </section>

    <div
      v-if="pendingAction && confirmationConfig"
      class="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-5 py-8"
      role="dialog"
      aria-modal="true"
      aria-labelledby="post-action-title"
      aria-describedby="post-action-description"
      @keydown.esc.prevent="closeConfirmation"
    >
      <section class="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
        <h2 id="post-action-title" class="text-2xl font-black text-slate-950">
          {{ confirmationConfig.title }}
        </h2>
        <p id="post-action-description" class="mt-3 leading-7 text-slate-700">
          {{ confirmationConfig.description }}
        </p>

        <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            ref="cancelButton"
            type="button"
            class="rounded-2xl bg-slate-100 px-5 py-3 text-sm font-black text-slate-800 transition hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
            :disabled="isSubmittingAction"
            @click="closeConfirmation"
          >
            Cancelar
          </button>
          <button
            type="button"
            class="rounded-2xl px-5 py-3 text-sm font-black text-white shadow-sm transition focus-visible:outline-2 focus-visible:outline-offset-4 disabled:cursor-not-allowed disabled:bg-slate-400"
            :class="confirmationConfig.confirmClass"
            :disabled="isSubmittingAction"
            @click="confirmAction"
          >
            {{ isSubmittingAction ? 'Procesando...' : confirmationConfig.confirmLabel }}
          </button>
        </div>
      </section>
    </div>

    <div
      v-if="isPreviewOpen && post"
      class="fixed inset-0 z-50 grid place-items-center bg-slate-950/70 px-4 py-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="post-preview-title"
      @keydown.esc.prevent="closePreview"
    >
      <section class="flex max-h-[90vh] w-full max-w-5xl flex-col rounded-[2rem] bg-slate-50 shadow-2xl">
        <header class="flex flex-col gap-4 border-b border-slate-200 px-5 py-4 sm:flex-row sm:items-start sm:justify-between sm:px-7">
          <div>
            <p class="text-xs font-black uppercase text-emerald-700">
              Vista previa
            </p>
            <h2 id="post-preview-title" class="mt-1 text-2xl font-black text-slate-950">
              {{ form.title.trim() || 'Sin título' }}
            </h2>
            <p class="mt-2 text-sm font-bold text-slate-600">
              Se muestra el estado actual del editor, aunque todavía no esté guardado.
            </p>
          </div>

          <button
            ref="previewCloseButton"
            type="button"
            class="self-start rounded-2xl bg-slate-900 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-slate-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-slate-900"
            @click="closePreview"
          >
            Cerrar
          </button>
        </header>

        <div class="min-h-0 overflow-y-auto px-5 py-6 sm:px-7">
          <article class="mx-auto max-w-3xl">
            <div class="mb-5 flex flex-wrap items-center gap-2 text-xs font-black uppercase">
              <span v-if="selectedSection" class="rounded-full bg-emerald-100 px-3 py-1 text-emerald-900">
                {{ selectedSection.name }}
              </span>
              <span
                v-for="tag in selectedTags"
                :key="tag.id"
                class="rounded-full bg-sky-100 px-3 py-1 text-sky-950"
              >
                #{{ tag.name }}
              </span>
            </div>

            <PostContentRenderer
              :document="form.contentDocument"
              :images="post.images"
              class="border-slate-200"
            />
          </article>
        </div>
      </section>
    </div>
  </main>
</template>
