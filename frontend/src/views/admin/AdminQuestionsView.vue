<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { getQuestionsByStatus } from '@/services/api/adminService'
import { signOut } from '@/services/firebase/authService'
import type { AdminQuestionStatus, AdminQuestionsResponse } from '@/types/adminQuestion'
import { adminQuestionStatusLabel } from '@/utils/adminQuestionStatus'

const PAGE_SIZE = 20

const QUESTION_TABS = [
  {
    query: 'pendientes',
    label: 'Pendientes',
    status: 'PENDING',
    emptyMessage: 'No hay preguntas pendientes.',
    loadingMessage: 'Cargando preguntas pendientes...',
  },
  {
    query: 'archivadas',
    label: 'Archivadas',
    status: 'ARCHIVED',
    emptyMessage: 'No hay preguntas archivadas.',
    loadingMessage: 'Cargando preguntas archivadas...',
  },
  {
    query: 'rechazadas',
    label: 'Rechazadas',
    status: 'REJECTED',
    emptyMessage: 'No hay preguntas rechazadas.',
    loadingMessage: 'Cargando preguntas rechazadas...',
  },
  {
    query: 'publicadas',
    label: 'Publicadas',
    status: 'PUBLISHED',
    emptyMessage: 'No hay preguntas publicadas.',
    loadingMessage: 'Cargando preguntas publicadas...',
  },
] as const

type QuestionTab = (typeof QUESTION_TABS)[number]

const route = useRoute()
const router = useRouter()
const response = ref<AdminQuestionsResponse | null>(null)
const isLoading = ref(true)
const hasError = ref(false)

const activeTab = computed(() => findTab(route.query.estado) ?? QUESTION_TABS[0])
const items = computed(() => response.value?.items ?? [])
const currentPage = computed(() => response.value?.page ?? 0)
const totalPages = computed(() => response.value?.totalPages ?? 0)
const hasMultiplePages = computed(() => totalPages.value > 1)
const isEmpty = computed(() => !isLoading.value && !hasError.value && items.value.length === 0)

watch(
  () => route.query.estado,
  (estado) => {
    if (estado && !findTab(estado)) {
      void router.replace({ name: 'admin-questions', query: { estado: 'pendientes' } })
      return
    }

    void loadQuestions(0)
  },
  { immediate: true },
)

async function loadQuestions(page: number) {
  isLoading.value = true
  hasError.value = false

  try {
    response.value = await getQuestionsByStatus(activeTab.value.status as AdminQuestionStatus, page, PAGE_SIZE)
  } catch (error) {
    if (isAdminAuthorizationError(error)) {
      await signOut().catch(() => undefined)
      await router.push({ name: 'admin-login', query: { reason: 'forbidden' } })
      return
    }

    hasError.value = true
    response.value = null
  } finally {
    isLoading.value = false
  }
}

function previousPage() {
  if (currentPage.value > 0) {
    void loadQuestions(currentPage.value - 1)
  }
}

function nextPage() {
  if (currentPage.value + 1 < totalPages.value) {
    void loadQuestions(currentPage.value + 1)
  }
}

function findTab(value: unknown): QuestionTab | undefined {
  if (typeof value !== 'string') {
    return undefined
  }

  return QUESTION_TABS.find((tab) => tab.query === value)
}

function displayNickname(nickname: string | null): string {
  return nickname ?? 'Anónimo'
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
    <section class="mx-auto max-w-6xl">
      <header class="max-w-3xl">
        <p class="text-sm font-black uppercase text-emerald-700">Preguntas</p>
        <h1 class="mt-3 text-4xl font-black">Preguntas recibidas</h1>
        <p class="mt-4 text-lg leading-8 text-slate-700">
          Revisa, archiva o rechaza las dudas enviadas por estudiantes antes de convertirlas en
          contenido académico.
        </p>
      </header>

      <nav
        class="mt-8 flex flex-wrap gap-3"
        aria-label="Estado de preguntas"
      >
        <RouterLink
          v-for="tab in QUESTION_TABS"
          :key="tab.query"
          :to="{ name: 'admin-questions', query: { estado: tab.query } }"
          class="rounded-2xl px-5 py-3 text-sm font-black transition focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
          :class="
            activeTab.query === tab.query
              ? 'bg-emerald-700 text-white shadow-sm'
              : 'bg-white text-slate-700 ring-1 ring-slate-200 hover:bg-emerald-50 hover:text-emerald-900'
          "
          :aria-current="activeTab.query === tab.query ? 'page' : undefined"
        >
          {{ tab.label }}
        </RouterLink>
      </nav>

      <div
        v-if="isLoading"
        class="mt-8 rounded-3xl border border-sky-100 bg-sky-50 px-6 py-10 text-center font-bold text-sky-950"
        role="status"
      >
        {{ activeTab.loadingMessage }}
      </div>

      <div
        v-else-if="hasError"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
        role="alert"
      >
        <p class="text-lg font-black text-red-950">No pudimos cargar las preguntas.</p>
        <p class="mt-2 text-sm leading-6 text-red-800">Intenta nuevamente en unos momentos.</p>
      </div>

      <div
        v-else-if="isEmpty"
        class="mt-8 rounded-3xl border border-emerald-100 bg-emerald-50 px-6 py-10 text-center text-lg font-bold text-emerald-950"
      >
        {{ activeTab.emptyMessage }}
      </div>

      <template v-else>
        <div class="mt-8 grid gap-5">
          <article
            v-for="question in items"
            :key="question.id"
            class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg"
          >
            <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
              <div>
                <div class="flex flex-wrap items-center gap-2 text-xs font-black uppercase">
                  <span class="rounded-full bg-emerald-100 px-3 py-1 text-emerald-900">
                    {{ adminQuestionStatusLabel(question.status) }}
                  </span>
                  <span class="rounded-full bg-sky-100 px-3 py-1 text-sky-950">
                    {{ question.section.name }}
                  </span>
                  <span
                    v-if="question.hasAttachment"
                    class="rounded-full bg-cyan-100 px-3 py-1 text-cyan-950"
                  >
                    Con imagen
                  </span>
                  <span
                    v-if="question.hasLinkedPost"
                    class="rounded-full bg-sky-100 px-3 py-1 text-sky-950"
                  >
                    {{
                      question.status === 'PUBLISHED'
                        ? 'Generó una publicación'
                        : 'Borrador en preparación'
                    }}
                  </span>
                </div>

                <p class="mt-4 text-sm font-bold text-slate-500">
                  {{ displayNickname(question.nickname) }} · {{ formatDate(question.createdAt) }}
                </p>
                <p class="mt-3 max-w-3xl text-lg leading-8 text-slate-800">
                  {{ question.questionPreview }}
                </p>
              </div>

              <RouterLink
                :to="{
                  name: 'admin-question-detail',
                  params: { id: question.id },
                  query: { estado: activeTab.query },
                }"
                class="inline-flex shrink-0 rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950"
              >
                Ver pregunta
              </RouterLink>
            </div>
          </article>
        </div>

        <nav
          v-if="hasMultiplePages"
          class="mt-8 flex flex-col items-center justify-between gap-4 rounded-3xl bg-white p-4 shadow-sm ring-1 ring-slate-200 sm:flex-row"
          :aria-label="`Paginación de preguntas ${activeTab.label.toLowerCase()}`"
        >
          <button
            type="button"
            class="rounded-2xl bg-slate-100 px-5 py-3 text-sm font-black text-slate-800 transition hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
            :disabled="currentPage === 0 || isLoading"
            @click="previousPage"
          >
            Anterior
          </button>

          <p class="text-sm font-black text-slate-700">
            Página {{ currentPage + 1 }} de {{ totalPages }}
          </p>

          <button
            type="button"
            class="rounded-2xl bg-slate-100 px-5 py-3 text-sm font-black text-slate-800 transition hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
            :disabled="currentPage + 1 >= totalPages || isLoading"
            @click="nextPage"
          >
            Siguiente
          </button>
        </nav>
      </template>
    </section>
  </main>
</template>
