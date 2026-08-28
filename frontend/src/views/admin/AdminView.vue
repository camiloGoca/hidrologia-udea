<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { getAdminAnalyticsSummary } from '@/services/api/adminAnalyticsService'
import { getPostsByStatus } from '@/services/api/adminPostService'
import { signOut } from '@/services/firebase/authService'

const router = useRouter()
const isLoading = ref(true)
const hasError = ref(false)
const pendingQuestions = ref(0)
const publishedPosts = ref(0)
const visitsToday = ref(0)

const metrics = computed(() => [
  {
    label: 'Preguntas pendientes',
    value: pendingQuestions.value,
    to: { name: 'admin-questions', query: { estado: 'pendientes' } },
  },
  {
    label: 'Publicaciones visibles',
    value: publishedPosts.value,
    to: { name: 'admin-posts', query: { estado: 'publicadas' } },
  },
  {
    label: 'Visitas hoy',
    value: visitsToday.value,
    to: { name: 'admin-analytics' },
  },
])

const quickActions = [
  {
    title: 'Preguntas',
    description: 'Revisa, publica, archiva o rechaza preguntas enviadas por estudiantes.',
    to: { name: 'admin-questions' },
  },
  {
    title: 'Publicaciones',
    description: 'Gestiona borradores, publicaciones visibles y contenido archivado.',
    to: { name: 'admin-posts' },
  },
  {
    title: 'Hashtags',
    description: 'Crea etiquetas, renómbralas y controla su uso en publicaciones.',
    to: { name: 'admin-hashtags' },
  },
  {
    title: 'Enlaces',
    description: 'Administra recursos externos visibles para estudiantes.',
    to: { name: 'admin-links' },
  },
  {
    title: 'Estadísticas',
    description: 'Consulta visitas, secciones populares y preguntas recibidas.',
    to: { name: 'admin-analytics' },
  },
]

void loadDashboard()

async function loadDashboard() {
  isLoading.value = true
  hasError.value = false

  try {
    const [summary, publishedResponse] = await Promise.all([
      getAdminAnalyticsSummary(),
      getPostsByStatus('PUBLISHED', 0, 1),
    ])

    pendingQuestions.value = summary.questions.pending
    publishedPosts.value = publishedResponse.totalElements
    visitsToday.value = summary.visitsToday
  } catch (error) {
    if (isAdminAuthorizationError(error)) {
      await signOut().catch(() => undefined)
      await router.push({ name: 'admin-login', query: { reason: 'forbidden' } })
      return
    }

    hasError.value = true
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <main class="px-5 py-10 sm:px-6">
    <section class="mx-auto max-w-6xl">
      <header class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200 sm:p-8">
        <p class="text-sm font-black uppercase text-emerald-700">Panel privado</p>
        <div class="mt-3 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h1 class="text-4xl font-black">Panel administrativo</h1>
            <p class="mt-3 max-w-2xl text-lg leading-8 text-slate-700">
              Una vista rápida para revisar pendientes, continuar la edición académica y consultar
              actividad reciente del sitio.
            </p>
          </div>

          <RouterLink
            :to="{ name: 'admin-posts' }"
            class="inline-flex w-fit rounded-2xl bg-emerald-800 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-emerald-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
          >
            Ir a publicaciones
          </RouterLink>
        </div>
      </header>

      <div
        v-if="isLoading"
        class="mt-8 rounded-3xl border border-sky-100 bg-sky-50 px-6 py-10 text-center font-bold text-sky-950"
        role="status"
      >
        Cargando resumen...
      </div>

      <div
        v-else-if="hasError"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
        role="alert"
      >
        <p class="text-lg font-black text-red-950">No pudimos cargar el resumen del panel.</p>
        <p class="mt-2 text-sm leading-6 text-red-800">Puedes entrar directamente a cada módulo.</p>
      </div>

      <section v-else class="mt-8 grid gap-4 md:grid-cols-3" aria-label="Resumen administrativo">
        <RouterLink
          v-for="metric in metrics"
          :key="metric.label"
          :to="metric.to"
          class="rounded-3xl border border-emerald-100 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
        >
          <p class="text-sm font-black uppercase text-emerald-700">{{ metric.label }}</p>
          <p class="mt-3 text-4xl font-black text-slate-950">{{ metric.value }}</p>
        </RouterLink>
      </section>

      <section class="mt-8 grid gap-5 lg:grid-cols-2" aria-labelledby="admin-actions-title">
        <div class="lg:col-span-2">
          <p class="text-sm font-black uppercase text-sky-800">Accesos rápidos</p>
          <h2 id="admin-actions-title" class="mt-2 text-3xl font-black text-slate-950">
            Gestión del curso
          </h2>
        </div>

        <RouterLink
          v-for="action in quickActions"
          :key="action.title"
          :to="action.to"
          class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:border-emerald-200 hover:shadow-lg focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
        >
          <div class="flex items-start justify-between gap-4">
            <div>
              <h3 class="text-2xl font-black text-slate-950">{{ action.title }}</h3>
              <p class="mt-3 leading-7 text-slate-700">{{ action.description }}</p>
            </div>
            <span class="rounded-full bg-emerald-100 px-3 py-1 text-sm font-black text-emerald-900">
              Abrir
            </span>
          </div>
        </RouterLink>
      </section>
    </section>
  </main>
</template>
