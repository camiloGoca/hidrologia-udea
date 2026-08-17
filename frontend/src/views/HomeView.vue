<script setup lang="ts">
import FeatureCard from '@/components/FeatureCard.vue'

const featureCards = [
  {
    title: 'Enlaces de interés',
    description: 'Recursos recomendados para profundizar conceptos y herramientas de la materia.',
    to: { name: 'links' },
    icon: 'links' as const,
  },
  {
    title: 'Talleres',
    description: 'Acceso organizado a las secciones de trabajo práctico del curso.',
    to: { name: 'workshops' },
    icon: 'workshops' as const,
  },
  {
    title: 'Parciales',
    description: 'Punto de entrada para consultar materiales relacionados con evaluaciones.',
    to: { name: 'exams' },
    icon: 'exams' as const,
  },
]
</script>

<template>
  <div>
    <section class="relative overflow-hidden bg-sky-950 text-white">
      <div class="absolute inset-0 bg-gradient-to-br from-emerald-900 via-sky-900 to-cyan-800" />
      <div
        class="absolute inset-x-0 bottom-0 h-28 bg-gradient-to-t from-slate-50 to-transparent"
        aria-hidden="true"
      />

      <div class="relative mx-auto grid max-w-6xl gap-10 px-5 py-20 sm:px-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div class="max-w-3xl">
          <p class="text-sm font-semibold uppercase text-emerald-100">Universidad de Antioquia</p>
          <h1 class="mt-4 text-4xl font-bold sm:text-6xl">Hidrología UdeA</h1>
          <p class="mt-6 max-w-xl text-2xl font-semibold text-cyan-50">
            Conocimiento que fluye, ciencia que transforma.
          </p>
          <p class="mt-5 max-w-2xl text-lg leading-8 text-sky-50">
            Espacio académico de Hidrología de la Universidad de Antioquia con recursos,
            talleres, parciales y respuestas a preguntas frecuentes.
          </p>

          <div class="mt-8 flex flex-col gap-3 sm:flex-row">
            <RouterLink
              :to="{ name: 'workshops' }"
              class="rounded-md bg-white px-5 py-3 text-center font-semibold text-sky-950 hover:bg-emerald-50 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
            >
              Ver talleres
            </RouterLink>
            <RouterLink
              :to="{ name: 'new-question' }"
              class="rounded-md border border-white/70 px-5 py-3 text-center font-semibold text-white hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
            >
              Agregar una pregunta
            </RouterLink>
          </div>
        </div>

        <div
          class="min-h-72 rounded-md border border-white/20 bg-white/10 p-6 shadow-2xl backdrop-blur"
          aria-label="Representación visual temporal de una cuenca hidrográfica"
        >
          <div class="grid h-full place-items-center rounded-md border border-white/20 bg-sky-950/20 p-6">
            <svg
              class="h-56 w-full max-w-sm text-cyan-50"
              viewBox="0 0 320 220"
              fill="none"
              aria-hidden="true"
            >
              <path
                d="M20 170C62 155 76 118 114 109C146 101 163 124 192 111C229 94 230 50 300 36"
                stroke="currentColor"
                stroke-width="10"
                stroke-linecap="round"
              />
              <path
                d="M65 54C92 71 98 93 112 109M150 44C171 65 171 90 192 111M248 86C227 89 209 101 192 111"
                stroke="currentColor"
                stroke-width="5"
                stroke-linecap="round"
                opacity="0.75"
              />
              <path
                d="M45 184H274"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                opacity="0.45"
              />
            </svg>
          </div>
        </div>
      </div>
    </section>

    <section class="mx-auto max-w-6xl px-5 py-14 sm:px-6">
      <div class="max-w-2xl">
        <p class="text-sm font-semibold uppercase text-emerald-800">Explorar</p>
        <h2 class="mt-3 text-3xl font-bold text-slate-950">Accesos principales</h2>
      </div>

      <div class="mt-8 grid gap-5 md:grid-cols-3">
        <FeatureCard
          v-for="card in featureCards"
          :key="card.title"
          :title="card.title"
          :description="card.description"
          :to="card.to"
          :icon="card.icon"
        />
      </div>
    </section>

    <section class="bg-white">
      <div class="mx-auto max-w-6xl px-5 py-14 sm:px-6">
        <div class="rounded-md border border-sky-100 bg-sky-50 p-6 sm:p-8">
          <p class="text-sm font-semibold uppercase text-sky-800">Participación estudiantil</p>
          <h2 class="mt-3 text-3xl font-bold text-slate-950">¿No encontraste tu duda?</h2>
          <p class="mt-3 max-w-2xl leading-7 text-slate-700">
            En una etapa posterior podrás enviar una pregunta al profesor desde esta sección.
          </p>
          <RouterLink
            :to="{ name: 'new-question' }"
            class="mt-6 inline-flex rounded-md bg-sky-800 px-5 py-3 font-semibold text-white hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
          >
            Agregar una pregunta
          </RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>
