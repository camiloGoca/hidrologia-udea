<script setup lang="ts">
import { computed } from 'vue'
import type { RouteLocationRaw } from 'vue-router'

import type { Section } from '@/types/section'

const props = defineProps<{
  section: Section
  to: RouteLocationRaw
  position: number
}>()

const defaultTone = {
  from: '#047857',
  to: '#064e3b',
  numberColor: '#064e3b',
  accentColor: '#d1fae5',
}

const tones = [
  defaultTone,
  {
    from: '#075985',
    to: '#0f172a',
    numberColor: '#0c4a6e',
    accentColor: '#e0f2fe',
  },
  {
    from: '#0f766e',
    to: '#164e63',
    numberColor: '#134e4a',
    accentColor: '#ccfbf1',
  },
]

const tone = computed(() => tones[(props.position - 1) % tones.length] ?? defaultTone)
const isWorkshop = computed(() => props.section.type === 'TALLER')
const cardStyle = computed(() => ({
  '--card-from': tone.value.from,
  '--card-to': tone.value.to,
  '--number-color': tone.value.numberColor,
  '--accent-color': tone.value.accentColor,
}))
</script>

<template>
  <RouterLink
    :to="to"
    class="section-card group relative flex min-h-80 overflow-hidden p-5 text-white transition duration-200 hover:-translate-y-1 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700 sm:min-h-[22rem] sm:p-7"
    :style="cardStyle"
  >
    <span
      class="absolute -right-14 -top-14 size-44 rounded-full border border-white/20 sm:size-52"
      aria-hidden="true"
    />
    <span class="absolute inset-x-6 bottom-0 h-28 rounded-t-full bg-white/5" aria-hidden="true" />

    <span class="relative flex h-full w-full flex-col">
      <span class="flex items-center justify-between gap-4">
        <span
          class="grid size-12 place-items-center rounded-full bg-white text-xl font-black leading-none shadow-md ring-1 ring-white/60 sm:size-16 sm:text-2xl"
          aria-hidden="true"
        >
          <span class="translate-y-px" :style="{ color: tone.numberColor }">{{ position }}</span>
        </span>
        <span class="rounded-full bg-white/12 px-3 py-1 text-xs font-black uppercase text-white/80">
          {{ section.slug }}
        </span>
      </span>

      <span class="mt-6 grid min-h-24 place-items-center sm:mt-7 sm:min-h-28" aria-hidden="true">
        <svg
          v-if="isWorkshop && position === 1"
          class="h-24 w-full max-w-44 sm:h-28 sm:max-w-52"
          viewBox="0 0 220 140"
          fill="none"
          stroke="currentColor"
          stroke-width="5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M16 104L58 58L92 88L130 36L204 104" />
          <path d="M36 116C68 100 92 101 118 116C146 132 169 128 194 112" />
          <path d="M108 116C108 98 118 84 136 70" />
        </svg>

        <svg
          v-else-if="isWorkshop && position === 2"
          class="h-24 w-full max-w-44 sm:h-28 sm:max-w-52"
          viewBox="0 0 220 140"
          fill="none"
          stroke="currentColor"
          stroke-width="5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M30 112V34" />
          <path d="M30 112H194" />
          <path d="M60 100V74" />
          <path d="M92 100V54" />
          <path d="M124 100V78" />
          <path d="M156 100V46" />
          <path d="M52 42C78 62 101 66 124 58C148 50 164 58 188 78" />
        </svg>

        <svg
          v-else-if="isWorkshop"
          class="h-24 w-full max-w-44 sm:h-28 sm:max-w-52"
          viewBox="0 0 220 140"
          fill="none"
          stroke="currentColor"
          stroke-width="5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M24 112H196" />
          <path d="M34 106C60 76 83 84 108 62C132 40 154 48 186 26" />
          <path d="M58 101H75" />
          <path d="M92 82H109" />
          <path d="M130 58H147" />
          <path d="M166 38H184" />
        </svg>

        <svg
          v-else-if="position === 1"
          class="h-24 w-full max-w-44 sm:h-28 sm:max-w-52"
          viewBox="0 0 220 140"
          fill="none"
          stroke="currentColor"
          stroke-width="5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M56 24H160L180 44V118H56V24Z" />
          <path d="M160 24V46H180" />
          <path d="M78 66H142" />
          <path d="M78 88H160" />
          <path d="M78 110H124" />
        </svg>

        <svg
          v-else-if="position === 2"
          class="h-24 w-full max-w-44 sm:h-28 sm:max-w-52"
          viewBox="0 0 220 140"
          fill="none"
          stroke="currentColor"
          stroke-width="5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M34 112H190" />
          <path d="M46 102V78" />
          <path d="M78 102V54" />
          <path d="M110 102V70" />
          <path d="M142 102V42" />
          <path d="M58 36H166" />
          <path d="M166 36L154 24" />
          <path d="M166 36L154 48" />
        </svg>

        <svg
          v-else
          class="h-24 w-full max-w-44 sm:h-28 sm:max-w-52"
          viewBox="0 0 220 140"
          fill="none"
          stroke="currentColor"
          stroke-width="5"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M30 112H190" />
          <path d="M38 104C64 78 88 86 110 66C132 46 154 48 184 28" />
          <path d="M72 98A10 10 0 1 0 72 78A10 10 0 0 0 72 98Z" />
          <path d="M128 74A10 10 0 1 0 128 54A10 10 0 0 0 128 74Z" />
          <path d="M176 40A10 10 0 1 0 176 20A10 10 0 0 0 176 40Z" />
        </svg>
      </span>

      <span class="mt-5 block sm:mt-6">
        <span class="block text-2xl font-black leading-tight sm:text-3xl">{{ section.name }}</span>
        <span v-if="section.description" class="mt-3 block leading-7 text-white/85 sm:mt-4">
          {{ section.description }}
        </span>
        <span v-else class="mt-3 block leading-7 text-white/75 sm:mt-4">
          Las publicaciones asociadas se mostrarán en una etapa posterior.
        </span>
      </span>

      <span class="mt-auto flex items-center justify-between pt-6 text-sm font-black uppercase text-white sm:pt-7">
        <span :style="{ color: tone.accentColor }">Ver publicaciones</span>
        <span
          class="grid size-10 place-items-center rounded-full bg-white text-lg text-slate-950 shadow-sm transition group-hover:translate-x-1 sm:size-11"
          aria-hidden="true"
        >
          →
        </span>
      </span>
    </span>
  </RouterLink>
</template>

<style scoped>
.section-card {
  border-radius: 2rem;
  box-shadow:
    0 18px 42px rgba(15, 23, 42, 0.16),
    inset 0 0 0 1px rgba(255, 255, 255, 0.16);
  background:
    radial-gradient(circle at 82% 12%, rgba(255, 255, 255, 0.2), transparent 28%),
    linear-gradient(145deg, var(--card-from), var(--card-to));
}

.section-card:hover {
  box-shadow:
    0 24px 54px rgba(15, 23, 42, 0.22),
    inset 0 0 0 1px rgba(255, 255, 255, 0.22);
}

@media (min-width: 640px) {
  .section-card {
    border-radius: 2.4rem;
  }
}
</style>
