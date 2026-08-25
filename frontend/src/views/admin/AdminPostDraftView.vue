<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { discardQuestionDraft, getAdminPost } from '@/services/api/adminService'
import { signOut } from '@/services/firebase/authService'
import type { AdminPost } from '@/types/adminPost'
import { adminPostStatusLabel } from '@/utils/adminPostStatus'

const route = useRoute()
const router = useRouter()
const post = ref<AdminPost | null>(null)
const isLoading = ref(true)
const hasError = ref(false)
const isDiscardModalOpen = ref(false)
const isSubmittingDiscard = ref(false)
const discardError = ref(false)
const cancelButton = ref<HTMLButtonElement | null>(null)
let lastFocusedElement: HTMLElement | null = null

const sourceQuestion = computed(() => post.value?.sourceQuestion ?? null)
const displayNickname = computed(() => sourceQuestion.value?.nickname ?? 'Anónimo')
const displayTitle = computed(() => post.value?.title.trim() || 'Sin título')
const displayContent = computed(() => post.value?.content.trim() || 'Sin contenido todavía')
const questionRoute = computed(() =>
  sourceQuestion.value
    ? { name: 'admin-question-detail', params: { id: sourceQuestion.value.id } }
    : { name: 'admin-questions' },
)

void loadPost()

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
    post.value = await getAdminPost(id)
  } catch (error) {
    if (isAdminAuthorizationError(error)) {
      await signOut().catch(() => undefined)
      await router.push({ name: 'admin-login', query: { reason: 'forbidden' } })
      return
    }

    hasError.value = true
    post.value = null
  } finally {
    isLoading.value = false
  }
}

async function openDiscardModal() {
  discardError.value = false
  isDiscardModalOpen.value = true
  lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null
  await nextTick()
  cancelButton.value?.focus()
}

function closeDiscardModal() {
  if (isSubmittingDiscard.value) {
    return
  }

  isDiscardModalOpen.value = false
  lastFocusedElement?.focus()
}

async function confirmDiscard() {
  if (!sourceQuestion.value || isSubmittingDiscard.value) {
    return
  }

  isSubmittingDiscard.value = true
  discardError.value = false

  try {
    await discardQuestionDraft(sourceQuestion.value.id)
    await router.push({
      name: 'admin-question-detail',
      params: { id: sourceQuestion.value.id },
    })
  } catch (error) {
    if (isAdminAuthorizationError(error)) {
      await signOut().catch(() => undefined)
      await router.push({ name: 'admin-login', query: { reason: 'forbidden' } })
      return
    }

    discardError.value = true
  } finally {
    isSubmittingDiscard.value = false
  }
}
</script>

<template>
  <main class="px-5 py-10 sm:px-6">
    <section class="mx-auto max-w-5xl">
      <RouterLink
        :to="questionRoute"
        class="rounded-md text-sm font-black text-sky-800 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
      >
        Volver a la pregunta
      </RouterLink>

      <div
        v-if="isLoading"
        class="mt-8 rounded-3xl border border-sky-100 bg-sky-50 px-6 py-10 text-center font-bold text-sky-950"
        role="status"
      >
        Cargando borrador...
      </div>

      <div
        v-else-if="hasError || !post"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
        role="alert"
      >
        <p class="text-lg font-black text-red-950">No pudimos cargar este borrador.</p>
        <p class="mt-2 text-sm leading-6 text-red-800">
          Puede que no exista o que no esté disponible para tu sesión.
        </p>
      </div>

      <article v-else class="mt-8 overflow-hidden rounded-[2rem] bg-white shadow-sm ring-1 ring-slate-200">
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
              <p class="text-sm font-black uppercase text-emerald-700">Borrador de publicación</p>
              <h1 class="mt-2 text-3xl font-black sm:text-4xl">{{ displayTitle }}</h1>
            </div>

            <button
              v-if="post.status === 'DRAFT' && sourceQuestion"
              type="button"
              class="rounded-2xl bg-red-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-red-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-800 disabled:cursor-not-allowed disabled:bg-slate-400"
              :disabled="isSubmittingDiscard"
              @click="openDiscardModal"
            >
              Descartar borrador
            </button>
          </div>

          <p
            v-if="discardError"
            class="mt-5 rounded-2xl bg-red-50 px-4 py-3 text-sm font-bold text-red-900"
            role="alert"
          >
            No pudimos descartar el borrador. Intenta nuevamente en unos momentos.
          </p>
        </header>

        <div class="grid gap-8 p-6 sm:p-8">
          <section aria-labelledby="draft-content-title">
            <h2 id="draft-content-title" class="text-sm font-black uppercase text-emerald-700">
              Contenido
            </h2>
            <p class="mt-4 whitespace-pre-wrap text-lg leading-8 text-slate-800">
              {{ displayContent }}
            </p>
          </section>

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
      v-if="isDiscardModalOpen"
      class="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-5 py-8"
      role="dialog"
      aria-modal="true"
      aria-labelledby="discard-draft-title"
      aria-describedby="discard-draft-description"
      @keydown.esc.prevent="closeDiscardModal"
    >
      <section class="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
        <h2 id="discard-draft-title" class="text-2xl font-black text-slate-950">
          ¿Descartar este borrador?
        </h2>
        <p id="discard-draft-description" class="mt-3 leading-7 text-slate-700">
          El borrador se eliminará. La pregunta original y su imagen permanecerán intactas y
          seguirán pendientes.
        </p>

        <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            ref="cancelButton"
            type="button"
            class="rounded-2xl bg-slate-100 px-5 py-3 text-sm font-black text-slate-800 transition hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
            :disabled="isSubmittingDiscard"
            @click="closeDiscardModal"
          >
            Cancelar
          </button>
          <button
            type="button"
            class="rounded-2xl bg-red-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-red-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            :disabled="isSubmittingDiscard"
            @click="confirmDiscard"
          >
            {{ isSubmittingDiscard ? 'Procesando...' : 'Descartar borrador' }}
          </button>
        </div>
      </section>
    </div>
  </main>
</template>
