<script setup lang="ts">
import { computed } from 'vue'

import PageBanner from '@/components/PageBanner.vue'
import SectionCard from '@/components/SectionCard.vue'
import { useSections } from '@/composables/useSections'

const { isLoading, hasError, sectionsByType } = useSections()
const talleres = sectionsByType('TALLER')
const isEmpty = computed(() => !isLoading.value && !hasError.value && talleres.value.length === 0)
</script>

<template>
  <div>
    <PageBanner
      eyebrow="Trabajo práctico"
      title="Talleres"
      description="Selecciona una sección de taller para consultar sus publicaciones cuando el módulo de contenido esté disponible."
    />

    <section class="mx-auto max-w-6xl px-5 pb-16 pt-4 sm:px-6 sm:pb-20">
      <div class="max-w-2xl">
        <p class="text-sm font-black uppercase text-emerald-800">Contenido del curso</p>
        <h2 class="mt-3 text-4xl font-black text-slate-950">Selecciona un taller</h2>
      </div>

      <p v-if="isLoading" class="mt-8 rounded-md border border-sky-200 bg-sky-50 p-4 text-sky-900">
        Cargando secciones...
      </p>

      <p
        v-else-if="hasError"
        class="mt-8 rounded-md border border-red-200 bg-red-50 p-4 text-red-900"
      >
        No pudimos cargar los talleres. Intenta nuevamente en unos momentos.
      </p>

      <p v-else-if="isEmpty" class="mt-8 rounded-md border border-slate-200 bg-white p-4 text-slate-700">
        Todavía no hay talleres disponibles.
      </p>

      <div v-else class="mt-8 grid gap-6 md:grid-cols-3">
        <SectionCard
          v-for="(section, index) in talleres"
          :key="section.id"
          :section="section"
          :position="index + 1"
          :to="{ name: 'workshop-detail', params: { slug: section.slug } }"
        />
      </div>
    </section>
  </div>
</template>
