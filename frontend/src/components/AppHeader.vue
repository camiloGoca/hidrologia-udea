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
  <header class="sticky top-0 z-20 border-b border-emerald-900/10 bg-white/95 backdrop-blur">
    <nav
      class="mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-4 sm:px-6"
      aria-label="Navegación principal"
    >
      <RouterLink
        :to="{ name: 'home' }"
        class="flex items-center gap-3 rounded-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-700"
        @click="closeMenu"
      >
        <span
          class="grid size-10 place-items-center rounded-md bg-emerald-800 text-sm font-bold text-white"
          aria-hidden="true"
        >
          HU
        </span>
        <span class="text-lg font-bold text-slate-950">Hidrología UdeA</span>
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
            class="rounded-md px-1 py-2 text-sm font-semibold text-slate-700 underline-offset-8 hover:text-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700"
            active-class="text-emerald-800 underline"
            @click="closeMenu"
          >
            {{ link.label }}
          </RouterLink>

          <RouterLink
            :to="{ name: 'new-question' }"
            class="rounded-md bg-sky-800 px-4 py-2 text-center text-sm font-semibold text-white shadow-sm hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-700"
            @click="closeMenu"
          >
            Agregar una pregunta
          </RouterLink>
        </div>
      </div>
    </nav>
  </header>
</template>
