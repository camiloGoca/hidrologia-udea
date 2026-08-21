<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { getQuestionById } from '@/services/api/adminService'
import { signOut } from '@/services/firebase/authService'
import type { AdminQuestionDetail } from '@/types/adminQuestion'

const route = useRoute()
const router = useRouter()
const question = ref<AdminQuestionDetail | null>(null)
const isLoading = ref(true)
const hasError = ref(false)

const displayNickname = computed(() => question.value?.nickname ?? 'Anónimo')

onMounted(() => {
  void loadQuestion()
})

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
        :to="{ name: 'admin-questions' }"
        class="rounded-md text-sm font-black text-sky-800 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
      >
        Volver a preguntas pendientes
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
              {{ question.status }}
            </span>
            <span class="rounded-full bg-sky-100 px-3 py-1 text-sky-950">
              {{ question.section.name }}
            </span>
          </div>

          <h1 class="mt-5 text-3xl font-black sm:text-4xl">Pregunta de estudiante</h1>
          <p class="mt-3 text-sm font-bold text-slate-500">
            {{ displayNickname }} · {{ formatDate(question.createdAt) }}
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
  </main>
</template>
