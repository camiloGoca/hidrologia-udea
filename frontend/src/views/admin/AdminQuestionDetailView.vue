<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import {
  archiveQuestion,
  createQuestionDraft,
  deleteRejectedQuestion,
  discardQuestionDraft,
  getQuestionById,
  rejectQuestion,
  reopenQuestion,
} from '@/services/api/adminService'
import { signOut } from '@/services/firebase/authService'
import type { AdminQuestionDetail } from '@/types/adminQuestion'
import { adminPostStatusLabel } from '@/utils/adminPostStatus'
import { adminQuestionStatusLabel } from '@/utils/adminQuestionStatus'

type QuestionAction = 'createDraft' | 'discardDraft' | 'archive' | 'reject' | 'reopen' | 'delete'

const ACTION_CONFIG = {
  createDraft: {
    label: 'Crear borrador de publicación',
    title: '¿Crear un borrador a partir de esta pregunta?',
    description: 'La pregunta seguirá pendiente mientras preparas la publicación.',
    confirmLabel: 'Crear borrador',
    successMessage: '',
    buttonClass: 'bg-sky-950 hover:bg-sky-900 focus-visible:outline-sky-950',
    confirmClass: 'bg-sky-950 hover:bg-sky-900 focus-visible:outline-sky-950',
  },
  discardDraft: {
    label: 'Descartar borrador',
    title: '¿Descartar este borrador?',
    description:
      'El borrador se eliminará. La pregunta original y su imagen permanecerán intactas y seguirán pendientes.',
    confirmLabel: 'Descartar borrador',
    successMessage: 'El borrador fue descartado.',
    buttonClass: 'bg-red-700 hover:bg-red-800 focus-visible:outline-red-800',
    confirmClass: 'bg-red-700 hover:bg-red-800 focus-visible:outline-red-800',
  },
  archive: {
    label: 'Archivar',
    title: '¿Archivar esta pregunta?',
    description: 'La pregunta se cerrará sin crear una publicación.',
    confirmLabel: 'Archivar pregunta',
    successMessage: 'La pregunta fue archivada.',
    buttonClass: 'bg-emerald-700 hover:bg-emerald-800 focus-visible:outline-emerald-800',
    confirmClass: 'bg-emerald-700 hover:bg-emerald-800 focus-visible:outline-emerald-800',
  },
  reject: {
    label: 'Rechazar',
    title: '¿Rechazar esta pregunta?',
    description: 'La pregunta dejará de aparecer como pendiente, pero no se eliminará.',
    confirmLabel: 'Rechazar pregunta',
    successMessage: 'La pregunta fue rechazada.',
    buttonClass: 'bg-red-700 hover:bg-red-800 focus-visible:outline-red-800',
    confirmClass: 'bg-red-700 hover:bg-red-800 focus-visible:outline-red-800',
  },
  reopen: {
    label: 'Reabrir',
    title: '¿Reabrir esta pregunta?',
    description: 'Volverá a la lista de preguntas pendientes.',
    confirmLabel: 'Reabrir pregunta',
    successMessage: 'La pregunta fue reabierta.',
    buttonClass: 'bg-sky-950 hover:bg-sky-900 focus-visible:outline-sky-950',
    confirmClass: 'bg-sky-950 hover:bg-sky-900 focus-visible:outline-sky-950',
  },
  delete: {
    label: 'Eliminar definitivamente',
    title: '¿Eliminar esta pregunta definitivamente?',
    description:
      'La pregunta y su imagen adjunta, si existe, serán eliminadas. Esta acción no se puede deshacer.',
    confirmLabel: 'Eliminar definitivamente',
    successMessage: '',
    buttonClass: 'bg-red-800 hover:bg-red-900 focus-visible:outline-red-900',
    confirmClass: 'bg-red-800 hover:bg-red-900 focus-visible:outline-red-900',
  },
} as const

const route = useRoute()
const router = useRouter()
const question = ref<AdminQuestionDetail | null>(null)
const isLoading = ref(true)
const hasError = ref(false)
const pendingAction = ref<QuestionAction | null>(null)
const isSubmittingAction = ref(false)
const actionError = ref(false)
const actionSuccessMessage = ref('')
const cancelButton = ref<HTMLButtonElement | null>(null)
let lastFocusedElement: HTMLElement | null = null

const displayNickname = computed(() => question.value?.nickname ?? 'Anónimo')
const hasDraft = computed(() => question.value?.linkedPost?.status === 'DRAFT')
const currentActionConfig = computed(() =>
  pendingAction.value ? ACTION_CONFIG[pendingAction.value] : null,
)
const availableActions = computed<QuestionAction[]>(() => {
  if (!question.value) {
    return []
  }

  if (question.value.status === 'PENDING') {
    return hasDraft.value ? ['discardDraft'] : ['createDraft', 'archive', 'reject']
  }

  if (question.value.status === 'REJECTED') {
    return question.value.linkedPost ? [] : ['reopen', 'delete']
  }

  if (question.value.status === 'ARCHIVED') {
    return question.value.linkedPost ? [] : ['reopen']
  }

  return []
})
const backRoute = computed(() => ({
  name: 'admin-questions',
  query: { estado: tabQueryForStatus(question.value?.status) ?? routeTabQuery() ?? 'pendientes' },
}))

void loadQuestion()

async function loadQuestion() {
  const id = Number(route.params.id)

  if (!Number.isFinite(id)) {
    isLoading.value = false
    hasError.value = true
    return
  }

  isLoading.value = true
  hasError.value = false

  try {
    question.value = await getQuestionById(id)
  } catch (error) {
    if (isAdminAuthorizationError(error)) {
      await signOut().catch(() => undefined)
      await router.push({ name: 'admin-login', query: { reason: 'forbidden' } })
      return
    }

    hasError.value = true
    question.value = null
  } finally {
    isLoading.value = false
  }
}

async function openConfirmation(action: QuestionAction) {
  actionError.value = false
  pendingAction.value = action
  lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null
  await nextTick()
  cancelButton.value?.focus()
}

function closeConfirmation() {
  if (isSubmittingAction.value) {
    return
  }

  pendingAction.value = null
  lastFocusedElement?.focus()
}

async function confirmAction() {
  if (!question.value || !pendingAction.value || isSubmittingAction.value) {
    return
  }

  isSubmittingAction.value = true
  actionError.value = false
  actionSuccessMessage.value = ''

  try {
    if (pendingAction.value === 'createDraft') {
      const draft = await createQuestionDraft(question.value.id)
      pendingAction.value = null
      await router.push({ name: 'admin-post-detail', params: { id: draft.id } })
      return
    }

    if (pendingAction.value === 'discardDraft') {
      await discardQuestionDraft(question.value.id)
      question.value = {
        ...question.value,
        linkedPost: null,
      }
      actionSuccessMessage.value = ACTION_CONFIG[pendingAction.value].successMessage
      pendingAction.value = null
      return
    }

    if (pendingAction.value === 'delete') {
      await deleteRejectedQuestion(question.value.id)
      pendingAction.value = null
      await router.push({ name: 'admin-questions', query: { estado: 'rechazadas' } })
      return
    }

    const result = await runStatusAction(pendingAction.value, question.value.id)
    question.value = {
      ...question.value,
      status: result.status,
      updatedAt: result.updatedAt,
    }
    actionSuccessMessage.value = ACTION_CONFIG[pendingAction.value].successMessage
    pendingAction.value = null
  } catch (error) {
    if (isAdminAuthorizationError(error)) {
      await signOut().catch(() => undefined)
      await router.push({ name: 'admin-login', query: { reason: 'forbidden' } })
      return
    }

    actionError.value = true
  } finally {
    isSubmittingAction.value = false
  }
}

function runStatusAction(action: Exclude<QuestionAction, 'createDraft' | 'discardDraft' | 'delete'>, id: number) {
  switch (action) {
    case 'archive':
      return archiveQuestion(id)
    case 'reject':
      return rejectQuestion(id)
    case 'reopen':
      return reopenQuestion(id)
  }
}

function routeTabQuery(): string | null {
  return typeof route.query.estado === 'string' ? route.query.estado : null
}

function tabQueryForStatus(status: AdminQuestionDetail['status'] | undefined): string | null {
  switch (status) {
    case 'PENDING':
      return 'pendientes'
    case 'ARCHIVED':
      return 'archivadas'
    case 'REJECTED':
      return 'rechazadas'
    case 'PUBLISHED':
      return 'publicadas'
    default:
      return null
  }
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('es-CO', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <main class="px-5 py-10 sm:px-6">
    <section class="mx-auto max-w-5xl">
      <RouterLink
        :to="backRoute"
        class="rounded-md text-sm font-black text-sky-800 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
      >
        Volver a preguntas
      </RouterLink>

      <div
        v-if="isLoading"
        class="mt-8 rounded-3xl border border-sky-100 bg-sky-50 px-6 py-10 text-center font-bold text-sky-950"
        role="status"
      >
        Cargando pregunta...
      </div>

      <div
        v-else-if="hasError || !question"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
        role="alert"
      >
        <p class="text-lg font-black text-red-950">No pudimos cargar esta pregunta.</p>
        <p class="mt-2 text-sm leading-6 text-red-800">
          Puede que no exista o que no esté disponible para tu sesión.
        </p>
      </div>

      <article v-else class="mt-8 overflow-hidden rounded-[2rem] bg-white shadow-sm ring-1 ring-slate-200">
        <header class="border-b border-slate-200 p-6 sm:p-8">
          <div class="flex flex-wrap items-center gap-2 text-xs font-black uppercase">
            <span class="rounded-full bg-emerald-100 px-3 py-1 text-emerald-900">
              {{ adminQuestionStatusLabel(question.status) }}
            </span>
            <span class="rounded-full bg-sky-100 px-3 py-1 text-sky-950">
              {{ question.section.name }}
            </span>
          </div>

          <div class="mt-5 flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
            <div>
              <h1 class="text-3xl font-black sm:text-4xl">Pregunta de estudiante</h1>
              <p class="mt-3 text-sm font-bold text-slate-500">
                {{ displayNickname }} · {{ formatDate(question.createdAt) }}
              </p>
            </div>

            <div v-if="availableActions.length > 0" class="flex flex-wrap gap-3">
              <button
                v-for="action in availableActions"
                :key="action"
                type="button"
                class="rounded-2xl px-5 py-3 text-sm font-black text-white shadow-sm transition focus-visible:outline-2 focus-visible:outline-offset-4 disabled:cursor-not-allowed disabled:bg-slate-400"
                :class="ACTION_CONFIG[action].buttonClass"
                :disabled="isSubmittingAction"
                @click="openConfirmation(action)"
              >
                {{ ACTION_CONFIG[action].label }}
              </button>
            </div>
          </div>

          <div
            v-if="hasDraft && question.linkedPost"
            class="mt-5 rounded-3xl border border-sky-100 bg-sky-50 p-5"
          >
            <p class="text-sm font-black uppercase text-sky-950">Borrador en preparación</p>
            <p class="mt-2 text-sm leading-6 text-slate-700">
              Esta pregunta sigue pendiente mientras preparas el borrador de publicación.
            </p>
            <RouterLink
              :to="{ name: 'admin-post-detail', params: { id: question.linkedPost.id } }"
              class="mt-4 inline-flex rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950"
            >
              Ver borrador
            </RouterLink>
          </div>

          <div
            v-if="question.status === 'PUBLISHED' && question.linkedPost"
            class="mt-5 rounded-3xl border border-emerald-100 bg-emerald-50 p-5"
          >
            <p class="text-sm font-black uppercase text-emerald-900">Publicación asociada</p>
            <p class="mt-2 text-lg font-black text-slate-950">
              {{ question.linkedPost.title || 'Sin título' }}
            </p>
            <p class="mt-1 text-sm font-bold text-slate-600">
              Estado: {{ adminPostStatusLabel(question.linkedPost.status) }}
            </p>
            <div class="mt-4 flex flex-wrap gap-3">
              <RouterLink
                v-if="question.linkedPost.status === 'PUBLISHED'"
                :to="{ name: 'post-detail', params: { id: question.linkedPost.id } }"
                class="inline-flex rounded-2xl bg-emerald-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
              >
                Ver publicación
              </RouterLink>
              <RouterLink
                :to="{ name: 'admin-post-detail', params: { id: question.linkedPost.id } }"
                class="inline-flex rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950"
              >
                Ver en administración
              </RouterLink>
            </div>
          </div>

          <p
            v-if="actionSuccessMessage"
            class="mt-5 rounded-2xl bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-900"
            role="status"
          >
            {{ actionSuccessMessage }}
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
          <section aria-labelledby="question-content-title">
            <h2 id="question-content-title" class="text-sm font-black uppercase text-emerald-700">
              Texto enviado
            </h2>
            <p class="mt-4 whitespace-pre-wrap text-lg leading-8 text-slate-800">
              {{ question.question }}
            </p>
          </section>

          <section v-if="question.attachment" aria-labelledby="question-attachment-title">
            <h2 id="question-attachment-title" class="text-sm font-black uppercase text-emerald-700">
              Imagen adjunta
            </h2>
            <div class="mt-4 overflow-hidden rounded-3xl border border-slate-200 bg-slate-50 p-3">
              <img
                :src="question.attachment.secureUrl"
                alt="Imagen adjunta a la pregunta"
                class="h-auto max-h-[70vh] w-full object-contain"
              />
            </div>
            <a
              :href="question.attachment.secureUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="mt-4 inline-flex rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950"
            >
              Abrir imagen
            </a>
          </section>
        </div>
      </article>
    </section>

    <div
      v-if="pendingAction && currentActionConfig"
      class="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-5 py-8"
      role="dialog"
      aria-modal="true"
      aria-labelledby="question-action-title"
      aria-describedby="question-action-description"
      @keydown.esc.prevent="closeConfirmation"
    >
      <section class="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
        <h2 id="question-action-title" class="text-2xl font-black text-slate-950">
          {{ currentActionConfig.title }}
        </h2>
        <p id="question-action-description" class="mt-3 leading-7 text-slate-700">
          {{ currentActionConfig.description }}
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
            :class="currentActionConfig.confirmClass"
            :disabled="isSubmittingAction"
            @click="confirmAction"
          >
            {{ isSubmittingAction ? 'Procesando...' : currentActionConfig.confirmLabel }}
          </button>
        </div>
      </section>
    </div>
  </main>
</template>
