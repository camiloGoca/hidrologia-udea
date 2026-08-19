<script setup lang="ts">
import { ref } from 'vue'

const isMenuOpen = ref(false)

const navLinks = [
  { label: 'Inicio', to: { name: 'home' } },
  { label: 'Enlaces de interés', to: { name: 'links' } },
  { label: 'Talleres', to: { name: 'workshops' } },
  { label: 'Parciales', to: { name: 'exams' } },
]

function closeMenu() {
  isMenuOpen.value = false
}
</script>

<template>
  <header class="sticky top-0 z-20 border-b border-emerald-950/10 bg-white/95 shadow-sm backdrop-blur">
    <nav
      class="mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-3 sm:px-6"
      aria-label="Navegación principal"
    >
      <RouterLink
        :to="{ name: 'home' }"
        class="flex items-center gap-3 rounded-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-700"
        @click="closeMenu"
      >
        <span
          class="grid size-11 place-items-center rounded-lg bg-gradient-to-br from-emerald-800 to-sky-900 text-white shadow-sm"
          aria-hidden="true"
        >
          <svg
            class="size-7"
            viewBox="0 0 32 32"
            fill="none"
            stroke="currentColor"
            stroke-width="2.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M16 4C12 9 9 13.5 9 18a7 7 0 0 0 14 0c0-4.5-3-9-7-14Z" />
            <path d="M12.5 19c2.4 2 4.6 2 7 0" />
          </svg>
        </span>
        <span class="leading-tight">
          <span class="block text-lg font-black text-emerald-950">Hidrología</span>
          <span class="block text-xs font-bold uppercase text-sky-800">UdeA</span>
        </span>
      </RouterLink>

      <button
        type="button"
        class="inline-flex items-center rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700 md:hidden"
        :aria-expanded="isMenuOpen"
        aria-controls="primary-navigation"
        @click="isMenuOpen = !isMenuOpen"
      >
        Menú
      </button>

      <div
        id="primary-navigation"
        :class="[
          'absolute left-0 right-0 top-full border-b border-slate-200 bg-white px-5 py-4 shadow-sm md:static md:block md:border-0 md:bg-transparent md:p-0 md:shadow-none',
          isMenuOpen ? 'block' : 'hidden',
        ]"
      >
        <div class="flex flex-col gap-3 md:flex-row md:items-center md:gap-6">
          <RouterLink
            v-for="link in navLinks"
            :key="link.label"
            :to="link.to"
            class="rounded-md px-1 py-2 text-sm font-bold text-slate-700 underline-offset-8 hover:text-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700"
            active-class="text-emerald-800 underline"
            @click="closeMenu"
          >
            {{ link.label }}
          </RouterLink>

          <RouterLink
            :to="{ name: 'new-question' }"
            class="rounded-md bg-emerald-800 px-4 py-2 text-center text-sm font-bold text-white shadow-sm hover:bg-emerald-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700"
            @click="closeMenu"
          >
            Agregar una pregunta
          </RouterLink>
        </div>
      </div>
    </nav>
  </header>
</template>
