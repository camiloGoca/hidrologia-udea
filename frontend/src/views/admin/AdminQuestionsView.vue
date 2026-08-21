<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { getPendingQuestions } from '@/services/api/adminService'
import { signOut } from '@/services/firebase/authService'
import type { AdminPendingQuestionsResponse } from '@/types/adminQuestion'

const PAGE_SIZE = 20

const router = useRouter()
const response = ref<AdminPendingQuestionsResponse | null>(null)
const isLoading = ref(true)
const hasError = ref(false)

const items = computed(() => response.value?.items ?? [])
const currentPage = computed(() => response.value?.page ?? 0)
const totalPages = computed(() => response.value?.totalPages ?? 0)
const hasMultiplePages = computed(() => totalPages.value > 1)
const isEmpty = computed(() => !isLoading.value && !hasError.value && items.value.length === 0)

onMounted(() => {
  void loadQuestions(0)
})

async function loadQuestions(page: number) {
  isLoading.value = true
  hasError.value = false

  try {
    response.value = await getPendingQuestions(page, PAGE_SIZE)
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
        <h1 class="mt-3 text-4xl font-black">Preguntas pendientes</h1>
        <p class="mt-4 text-lg leading-8 text-slate-700">
          Revisa las dudas enviadas por estudiantes antes de convertirlas en contenido académico.
        </p>
      </header>

      <div
        v-if="isLoading"
        class="mt-8 rounded-3xl border border-sky-100 bg-sky-50 px-6 py-10 text-center font-bold text-sky-950"
        role="status"
      >
        Cargando preguntas pendientes...
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
        No hay preguntas pendientes.
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
                    PENDIENTE
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
                </div>

                <p class="mt-4 text-sm font-bold text-slate-500">
                  {{ displayNickname(question.nickname) }} · {{ formatDate(question.createdAt) }}
                </p>
                <p class="mt-3 max-w-3xl text-lg leading-8 text-slate-800">
                  {{ question.questionPreview }}
                </p>
              </div>

              <RouterLink
                :to="{ name: 'admin-question-detail', params: { id: question.id } }"
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
          aria-label="Paginación de preguntas pendientes"
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
