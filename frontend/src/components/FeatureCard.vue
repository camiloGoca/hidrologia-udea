<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'

defineProps<{
  title: string
  description: string
  to: RouteLocationRaw
  icon: 'links' | 'workshops' | 'exams'
}>()
</script>

<template>
  <RouterLink
    :to="to"
    class="group flex min-h-56 flex-col justify-between rounded-md border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:border-emerald-700 hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-700"
  >
    <span
      class="grid size-12 place-items-center rounded-md bg-emerald-50 text-emerald-800 ring-1 ring-emerald-900/10"
      aria-hidden="true"
    >
      <svg
        v-if="icon === 'links'"
        class="size-6"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M10 13a5 5 0 0 0 7.07 0l2.12-2.12a5 5 0 0 0-7.07-7.07L11 4.93" />
        <path d="M14 11a5 5 0 0 0-7.07 0L4.81 13.12a5 5 0 0 0 7.07 7.07L13 19.07" />
      </svg>

      <svg
        v-else-if="icon === 'workshops'"
        class="size-6"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2Z" />
        <path d="M8 7h8" />
        <path d="M8 11h6" />
      </svg>

      <svg
        v-else
        class="size-6"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M8 2v4" />
        <path d="M16 2v4" />
        <path d="M3 10h18" />
        <rect width="18" height="18" x="3" y="4" rx="2" />
        <path d="M8 15h.01" />
        <path d="M12 15h.01" />
        <path d="M16 15h.01" />
      </svg>
    </span>

    <span>
      <span class="block text-2xl font-bold text-slate-950">{{ title }}</span>
      <span class="mt-3 block leading-7 text-slate-700">{{ description }}</span>
    </span>

    <span class="mt-6 text-sm font-semibold text-sky-800 group-hover:text-sky-950">
      Ir a la sección
    </span>
  </RouterLink>
</template>
