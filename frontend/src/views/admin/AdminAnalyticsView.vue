<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { getAdminAnalyticsSummary } from '@/services/api/adminAnalyticsService'
import { signOut } from '@/services/firebase/authService'
import type { AdminAnalyticsSection, AdminAnalyticsSummary } from '@/types/analytics'

const router = useRouter()
const summary = ref<AdminAnalyticsSummary | null>(null)
const isLoading = ref(true)
const hasError = ref(false)

const maxDailyVisits = computed(() =>
  Math.max(1, ...(summary.value?.dailyVisits.map((item) => item.visits) ?? [0])),
)
const kpis = computed(() => {
  if (!summary.value) {
    return []
  }

  return [
    { label: 'Total', value: summary.value.totalVisits },
    { label: 'Hoy', value: summary.value.visitsToday },
    { label: 'Esta semana', value: summary.value.visitsThisWeek },
    { label: 'Este mes', value: summary.value.visitsThisMonth },
  ]
})

void loadSummary()

async function loadSummary() {
  isLoading.value = true
  hasError.value = false

  try {
    summary.value = await getAdminAnalyticsSummary()
  } catch (error) {
    if (isAdminAuthorizationError(error)) {
      await signOut().catch(() => undefined)
      await router.push({ name: 'admin-login', query: { reason: 'forbidden' } })
      return
    }

    hasError.value = true
    summary.value = null
  } finally {
    isLoading.value = false
  }
}

function sectionStatus(section: AdminAnalyticsSection | null): string {
  return section ? `${section.name} · ${section.views} consultas` : 'Sin datos todavía'
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit',
    month: 'short',
  }).format(new Date(`${value}T00:00:00`))
}

function barHeight(visits: number): string {
  return `${Math.max(6, Math.round((visits / maxDailyVisits.value) * 100))}%`
}
</script>

<template>
  <main class="px-5 py-10 sm:px-6">
    <section class="mx-auto max-w-6xl">
      <header class="max-w-3xl">
        <p class="text-sm font-black uppercase text-emerald-700">Estadísticas</p>
        <h1 class="mt-3 text-4xl font-black">Resumen del sitio</h1>
        <p class="mt-4 text-lg leading-8 text-slate-700">
          Consulta métricas privadas de visitas, contenido consultado y preguntas recibidas.
        </p>
      </header>

      <div
        v-if="isLoading"
        class="mt-8 rounded-3xl border border-sky-100 bg-sky-50 px-6 py-10 text-center font-bold text-sky-950"
        role="status"
      >
        Cargando estadísticas...
      </div>

      <div
        v-else-if="hasError"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
        role="alert"
      >
        <p class="text-lg font-black text-red-950">No pudimos cargar las estadísticas.</p>
        <p class="mt-2 text-sm leading-6 text-red-800">Intenta nuevamente en unos momentos.</p>
      </div>

      <div v-else-if="summary" class="mt-8 space-y-8">
        <section class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4" aria-label="Indicadores principales">
          <article
            v-for="item in kpis"
            :key="item.label"
            class="rounded-3xl border border-emerald-100 bg-white p-6 shadow-sm"
          >
            <p class="text-sm font-black uppercase text-emerald-700">{{ item.label }}</p>
            <p class="mt-3 text-4xl font-black text-slate-950">{{ item.value }}</p>
          </article>
        </section>

        <section class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div class="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p class="text-sm font-black uppercase text-sky-800">Últimos 30 días</p>
              <h2 class="mt-2 text-2xl font-black text-slate-950">Visitas diarias</h2>
            </div>
            <p class="text-sm font-bold text-slate-500">Zona horaria: America/Bogota</p>
          </div>

          <div class="mt-6 flex h-56 items-end gap-1 overflow-x-auto pb-2" aria-label="Gráfica de visitas diarias">
            <div
              v-for="day in summary.dailyVisits"
              :key="day.date"
              class="flex min-w-8 flex-1 flex-col items-center justify-end gap-2"
            >
              <div
                class="w-full rounded-t-xl bg-emerald-600"
                :style="{ height: barHeight(day.visits) }"
                :aria-label="`${day.visits} visitas el ${formatDate(day.date)}`"
              />
              <span class="text-[10px] font-bold text-slate-500">{{ formatDate(day.date) }}</span>
            </div>
          </div>
        </section>

        <section class="grid gap-6 lg:grid-cols-2">
          <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <p class="text-sm font-black uppercase text-emerald-700">Secciones más consultadas</p>
            <div v-if="summary.mostViewedSections.length > 0" class="mt-5 grid gap-3">
              <div
                v-for="section in summary.mostViewedSections"
                :key="section.slug"
                class="rounded-2xl bg-slate-50 px-4 py-3"
              >
                <div class="flex items-center justify-between gap-4">
                  <div>
                    <p class="font-black text-slate-950">{{ section.name }}</p>
                    <p class="text-sm font-bold text-slate-500">{{ section.slug }}</p>
                  </div>
                  <span class="rounded-full bg-emerald-100 px-3 py-1 text-sm font-black text-emerald-900">
                    {{ section.views }}
                  </span>
                </div>
              </div>
            </div>
            <p v-else class="mt-5 rounded-2xl bg-slate-50 px-4 py-3 text-sm font-bold text-slate-600">
              Sin consultas de secciones todavía.
            </p>
          </article>

          <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <p class="text-sm font-black uppercase text-sky-800">Destacados</p>
            <div class="mt-5 grid gap-4">
              <div class="rounded-2xl bg-emerald-50 px-4 py-4">
                <p class="text-sm font-black uppercase text-emerald-700">Taller más consultado</p>
                <p class="mt-2 font-black text-slate-950">
                  {{ sectionStatus(summary.mostViewedWorkshop) }}
                </p>
              </div>
              <div class="rounded-2xl bg-cyan-50 px-4 py-4">
                <p class="text-sm font-black uppercase text-cyan-700">Parcial más consultado</p>
                <p class="mt-2 font-black text-slate-950">
                  {{ sectionStatus(summary.mostViewedExam) }}
                </p>
              </div>
            </div>
          </article>
        </section>

        <section class="grid gap-6 lg:grid-cols-[1fr_0.8fr]">
          <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <p class="text-sm font-black uppercase text-emerald-700">Publicaciones más consultadas</p>
            <div v-if="summary.mostViewedPosts.length > 0" class="mt-5 grid gap-3">
              <RouterLink
                v-for="post in summary.mostViewedPosts"
                :key="post.id"
                :to="{ name: 'post-detail', params: { id: post.id } }"
                class="rounded-2xl bg-slate-50 px-4 py-3 transition hover:bg-emerald-50 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-700"
              >
                <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p class="font-black text-slate-950">{{ post.title }}</p>
                    <p class="text-sm font-bold text-slate-500">{{ post.section.name }}</p>
                  </div>
                  <span class="text-sm font-black text-emerald-800">{{ post.views }} consultas</span>
                </div>
              </RouterLink>
            </div>
            <p v-else class="mt-5 rounded-2xl bg-slate-50 px-4 py-3 text-sm font-bold text-slate-600">
              Sin consultas de publicaciones todavía.
            </p>
          </article>

          <article class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <p class="text-sm font-black uppercase text-sky-800">Preguntas</p>
            <div class="mt-5 grid gap-3">
              <div class="rounded-2xl bg-slate-50 px-4 py-3">
                <p class="text-sm font-bold text-slate-500">Total recibidas</p>
                <p class="text-3xl font-black text-slate-950">{{ summary.questions.total }}</p>
              </div>
              <div class="rounded-2xl bg-amber-50 px-4 py-3">
                <p class="text-sm font-bold text-amber-800">Pendientes</p>
                <p class="text-3xl font-black text-slate-950">{{ summary.questions.pending }}</p>
              </div>
              <div class="rounded-2xl bg-emerald-50 px-4 py-3">
                <p class="text-sm font-bold text-emerald-800">Publicadas/respondidas</p>
                <p class="text-3xl font-black text-slate-950">{{ summary.questions.published }}</p>
              </div>
            </div>
          </article>
        </section>
      </div>
    </section>
  </main>
</template>
