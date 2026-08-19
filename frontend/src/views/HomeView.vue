<script setup lang="ts">
import FeatureCard from '@/components/FeatureCard.vue'
import rioImageUrl from '@/assets/images/rio-hidrologia.jpg'

const featureCards = [
  {
    title: 'Enlaces de interés',
    description: 'Recursos recomendados para profundizar conceptos, métodos y herramientas.',
    to: { name: 'links' },
    icon: 'links' as const,
  },
  {
    title: 'Talleres',
    description: 'Secciones de trabajo práctico para avanzar paso a paso en el curso.',
    to: { name: 'workshops' },
    icon: 'workshops' as const,
  },
  {
    title: 'Parciales',
    description: 'Material organizado para revisar evaluaciones y preparar conceptos clave.',
    to: { name: 'exams' },
    icon: 'exams' as const,
  },
]
</script>

<template>
  <div>
    <section
      class="home-hero relative isolate overflow-hidden bg-sky-950 text-white"
      :style="{ '--hero-image': `url(${rioImageUrl})` }"
    >
      <div class="relative mx-auto max-w-6xl px-5 py-20 sm:px-6 sm:py-24 lg:py-28">
        <div class="max-w-3xl">
          <p class="text-sm font-bold uppercase text-emerald-100">Universidad de Antioquia</p>
          <h1 class="mt-4 text-5xl font-black sm:text-7xl">Hidrología UdeA</h1>
          <p class="mt-6 max-w-xl text-2xl font-bold text-cyan-50 sm:text-3xl">
            Conocimiento que fluye, ciencia que transforma.
          </p>
          <p class="mt-5 max-w-2xl text-lg leading-8 text-sky-50">
            Espacio académico de Hidrología de la Universidad de Antioquia con recursos, talleres,
            parciales y respuestas a preguntas frecuentes.
          </p>

          <div class="mt-9 flex flex-col gap-3 sm:flex-row">
            <RouterLink
              :to="{ name: 'workshops' }"
              class="rounded-md bg-white px-6 py-3 text-center font-black text-sky-950 shadow-sm hover:bg-emerald-50 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
            >
              Ver talleres
            </RouterLink>
            <RouterLink
              :to="{ name: 'new-question' }"
              class="rounded-md border border-white/70 px-6 py-3 text-center font-black text-white hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
            >
              Agregar una pregunta
            </RouterLink>
          </div>
        </div>
      </div>
    </section>

    <section class="mx-auto max-w-6xl px-5 py-16 sm:px-6">
      <div class="max-w-2xl">
        <p class="text-sm font-black uppercase text-emerald-800">Explorar</p>
        <h2 class="mt-3 text-4xl font-black text-slate-950">Accesos principales</h2>
        <p class="mt-4 leading-7 text-slate-700">
          El punto de partida para consultar recursos, talleres y parciales del curso.
        </p>
      </div>

      <div class="mt-9 grid gap-6 md:grid-cols-3">
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
      <div class="mx-auto max-w-6xl px-5 py-16 sm:px-6">
        <div
          class="relative overflow-hidden rounded-lg border border-cyan-100 bg-gradient-to-br from-sky-50 to-emerald-50 p-7 shadow-sm sm:p-10"
        >
          <div
            class="absolute -right-10 -top-10 size-36 rounded-full border border-cyan-200"
            aria-hidden="true"
          />
          <p class="text-sm font-black uppercase text-sky-800">Participación estudiantil</p>
          <h2 class="mt-3 text-4xl font-black text-slate-950">¿No encontraste tu duda?</h2>
          <p class="mt-4 max-w-2xl leading-7 text-slate-700">
            Esta sección permitirá enviar una pregunta al profesor cuando implementemos el módulo
            correspondiente.
          </p>
          <RouterLink
            :to="{ name: 'new-question' }"
            class="mt-7 inline-flex rounded-md bg-sky-900 px-6 py-3 font-black text-white shadow-sm hover:bg-sky-950 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
          >
            Agregar una pregunta
          </RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-hero {
  background-image:
    linear-gradient(90deg, rgba(2, 44, 34, 0.9) 0%, rgba(8, 47, 73, 0.44) 48%, rgba(15, 23, 42, 0.08) 100%),
    var(--hero-image);
  background-position: center 48%;
  background-size: cover;
}

@media (max-width: 640px) {
  .home-hero {
    background-image:
      linear-gradient(90deg, rgba(2, 44, 34, 0.84) 0%, rgba(8, 47, 73, 0.5) 66%, rgba(15, 23, 42, 0.18) 100%),
      var(--hero-image);
    background-position: 58% center;
  }
}
</style>
