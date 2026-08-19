<script setup lang="ts">
defineProps<{
  sectionKind: 'taller' | 'parcial'
}>()
</script>

<template>
  <section class="mx-auto max-w-4xl px-5 py-12 sm:px-6 sm:py-16">
    <RouterLink
      :to="{ name: sectionKind === 'taller' ? 'workshops' : 'exams' }"
      class="rounded-md text-sm font-black text-sky-800 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
    >
      Volver a {{ sectionKind === 'taller' ? 'Talleres' : 'Parciales' }}
    </RouterLink>

    <div
      class="relative mt-6 overflow-hidden rounded-lg border border-cyan-100 bg-gradient-to-br from-white to-sky-50 p-7 shadow-sm sm:p-10"
    >
      <div class="absolute -right-10 -top-10 size-36 rounded-full border border-cyan-200" aria-hidden="true" />
      <p class="text-sm font-black uppercase text-emerald-800">
        {{ sectionKind === 'taller' ? 'Taller' : 'Parcial' }}
      </p>
      <h1 class="mt-3 text-4xl font-black text-slate-950">Publicaciones en preparación</h1>
      <p class="mt-4 max-w-2xl leading-7 text-slate-700">
        Aquí se mostrarán las publicaciones asociadas cuando implementemos el módulo de contenido.
      </p>
    </div>
  </section>
</template>
