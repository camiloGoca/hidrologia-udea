<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { getSections } from '@/services/api/sectionService'
import type { Section } from '@/types/section'

const sections = ref<Section[]>([])
const isLoading = ref(true)
const hasError = ref(false)

const talleres = computed(() =>
  sections.value.filter((section) => section.type === 'TALLER'),
)
const parciales = computed(() =>
  sections.value.filter((section) => section.type === 'PARCIAL'),
)
const isEmpty = computed(
  () => !isLoading.value && !hasError.value && sections.value.length === 0,
)

async function loadSections() {
  isLoading.value = true
  hasError.value = false

  try {
    sections.value = await getSections()
  } catch {
    hasError.value = true
    sections.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(loadSections)
</script>

<template>
  <main class="min-h-screen bg-slate-50 px-6 py-12 text-slate-950 sm:py-16">
    <section class="mx-auto flex max-w-5xl flex-col gap-8">
      <div class="space-y-3">
        <p class="text-sm font-semibold uppercase text-sky-700">Sections API</p>
        <h1 class="text-4xl font-bold">Hidrología UdeA</h1>
        <p class="max-w-2xl text-lg text-slate-700">
          Consulta inicial de secciones académicas desde el backend.
        </p>
      </div>

      <p v-if="isLoading" class="rounded-md border border-sky-200 bg-sky-50 p-4 text-sky-900">
        Cargando secciones...
      </p>

      <p
        v-else-if="hasError"
        class="rounded-md border border-red-200 bg-red-50 p-4 text-red-900"
      >
        No pudimos cargar las secciones. Intenta nuevamente en unos momentos.
      </p>

      <p
        v-else-if="isEmpty"
        class="rounded-md border border-slate-200 bg-white p-4 text-slate-700"
      >
        Todavía no hay secciones disponibles.
      </p>

      <div v-else class="grid gap-8 lg:grid-cols-2">
        <section class="space-y-4">
          <h2 class="text-2xl font-semibold">Talleres</h2>

          <ul class="grid gap-4">
            <li
              v-for="section in talleres"
              :key="section.id"
              class="rounded-md border border-slate-200 bg-white p-5 shadow-sm"
            >
              <h3 class="text-lg font-semibold">{{ section.name }}</h3>
              <p v-if="section.description" class="mt-2 text-slate-700">
                {{ section.description }}
              </p>
            </li>
          </ul>
        </section>

        <section class="space-y-4">
          <h2 class="text-2xl font-semibold">Parciales</h2>

          <ul class="grid gap-4">
            <li
              v-for="section in parciales"
              :key="section.id"
              class="rounded-md border border-slate-200 bg-white p-5 shadow-sm"
            >
              <h3 class="text-lg font-semibold">{{ section.name }}</h3>
              <p v-if="section.description" class="mt-2 text-slate-700">
                {{ section.description }}
              </p>
            </li>
          </ul>
        </section>
      </div>
    </section>
  </main>
</template>
