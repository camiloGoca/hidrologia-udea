<script setup lang="ts">
import { computed } from 'vue'

import SectionCard from '@/components/SectionCard.vue'
import { useSections } from '@/composables/useSections'

const { isLoading, hasError, sectionsByType } = useSections()
const parciales = sectionsByType('PARCIAL')
const isEmpty = computed(() => !isLoading.value && !hasError.value && parciales.value.length === 0)
</script>

<template>
  <section class="mx-auto max-w-6xl px-5 py-12 sm:px-6 sm:py-16">
    <div class="max-w-3xl">
      <p class="text-sm font-semibold uppercase text-emerald-800">Evaluaciones</p>
      <h1 class="mt-3 text-4xl font-bold text-slate-950">Parciales</h1>
      <p class="mt-4 text-lg leading-8 text-slate-700">
        Secciones de parciales cargadas desde el backend. Las publicaciones asociadas se
        incorporarán cuando implementemos ese módulo.
      </p>
    </div>

    <p v-if="isLoading" class="mt-8 rounded-md border border-sky-200 bg-sky-50 p-4 text-sky-900">
      Cargando secciones...
    </p>

    <p
      v-else-if="hasError"
      class="mt-8 rounded-md border border-red-200 bg-red-50 p-4 text-red-900"
    >
      No pudimos cargar los parciales. Intenta nuevamente en unos momentos.
    </p>

    <p v-else-if="isEmpty" class="mt-8 rounded-md border border-slate-200 bg-white p-4 text-slate-700">
      Todavía no hay parciales disponibles.
    </p>

    <div v-else class="mt-8 grid gap-5 md:grid-cols-3">
      <SectionCard
        v-for="section in parciales"
        :key="section.id"
        :section="section"
        :to="{ name: 'exam-detail', params: { slug: section.slug } }"
      />
    </div>
  </section>
</template>
