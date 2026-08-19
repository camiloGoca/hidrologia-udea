<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'

defineProps<{
  title: string
  description: string
  to: RouteLocationRaw
  icon: 'links' | 'workshops' | 'exams'
}>()

const styles = {
  links: {
    card: 'from-emerald-800 to-emerald-950 focus-visible:outline-emerald-700',
    icon: 'bg-emerald-100 text-emerald-900',
    action: 'text-emerald-100',
  },
  workshops: {
    card: 'from-sky-900 to-slate-950 focus-visible:outline-sky-700',
    icon: 'bg-sky-100 text-sky-950',
    action: 'text-sky-100',
  },
  exams: {
    card: 'from-teal-700 to-cyan-950 focus-visible:outline-teal-700',
    icon: 'bg-cyan-100 text-cyan-950',
    action: 'text-cyan-100',
  },
}
</script>

<template>
  <RouterLink
    :to="to"
    :class="[
      'group relative flex min-h-80 overflow-hidden rounded-lg bg-gradient-to-br p-7 text-white shadow-lg transition duration-200 hover:-translate-y-1 hover:shadow-xl focus-visible:outline-2 focus-visible:outline-offset-4',
      styles[icon].card,
    ]"
  >
    <span class="absolute -right-12 -top-12 size-40 rounded-full border border-white/20" aria-hidden="true" />
    <span class="absolute bottom-0 left-0 right-0 h-28 bg-white/5" aria-hidden="true" />

    <span class="relative flex h-full flex-col justify-between">
      <span
        :class="[
          'grid size-16 place-items-center rounded-lg shadow-sm ring-1 ring-white/25',
          styles[icon].icon,
        ]"
        aria-hidden="true"
      >
        <svg
          v-if="icon === 'links'"
          class="size-9"
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
          class="size-9"
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
          class="size-9"
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

      <span class="mt-12 block">
        <span class="block text-3xl font-black">{{ title }}</span>
        <span class="mt-4 block leading-7 text-white/85">{{ description }}</span>
      </span>

      <span :class="['mt-8 inline-flex items-center text-sm font-black uppercase', styles[icon].action]">
        Ingresar
        <span class="ml-2 transition group-hover:translate-x-1" aria-hidden="true">→</span>
      </span>
    </span>
  </RouterLink>
</template>
