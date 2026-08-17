<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'

import type { Section } from '@/types/section'

defineProps<{
  section: Section
  to: RouteLocationRaw
}>()
</script>

<template>
  <RouterLink
    :to="to"
    class="block rounded-md border border-slate-200 bg-white p-5 shadow-sm transition hover:border-sky-700 hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
  >
    <p class="text-xs font-semibold uppercase text-emerald-800">
      {{ section.slug }}
    </p>
    <h2 class="mt-2 text-xl font-bold text-slate-950">{{ section.name }}</h2>
    <p v-if="section.description" class="mt-3 leading-7 text-slate-700">
      {{ section.description }}
    </p>
    <p v-else class="mt-3 leading-7 text-slate-600">
      Las publicaciones asociadas se mostrarán en una etapa posterior.
    </p>
  </RouterLink>
</template>
