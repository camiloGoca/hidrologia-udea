<script setup lang="ts">
defineProps<{
  sectionKind: 'taller' | 'parcial'
}>()
</script>

<template>
  <section class="mx-auto max-w-4xl px-5 py-12 sm:px-6 sm:py-16">
    <RouterLink
      :to="{ name: sectionKind === 'taller' ? 'workshops' : 'exams' }"
      class="text-sm font-semibold text-sky-800 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
    >
      Volver a {{ sectionKind === 'taller' ? 'Talleres' : 'Parciales' }}
    </RouterLink>

    <div class="mt-6 rounded-md border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <p class="text-sm font-semibold uppercase text-emerald-800">
        {{ sectionKind === 'taller' ? 'Taller' : 'Parcial' }}
      </p>
      <h1 class="mt-3 text-3xl font-bold text-slate-950">Publicaciones en preparación</h1>
      <p class="mt-4 leading-7 text-slate-700">
        Aquí se mostrarán las publicaciones asociadas cuando implementemos el módulo de contenido.
      </p>
    </div>
  </section>
</template>
