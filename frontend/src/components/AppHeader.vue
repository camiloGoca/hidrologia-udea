<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import logoHeaderUrl from '@/assets/brand/hidrologia-logo-header.png'
import logoMarkUrl from '@/assets/brand/hidrologia-mark.png'

const route = useRoute()
const router = useRouter()
const isMenuOpen = ref(false)
const searchQuery = ref(String(route.query.q ?? ''))

const navLinks = [
  { label: 'Inicio', to: { name: 'home' } },
  { label: 'Enlaces de interés', to: { name: 'links' } },
  { label: 'Talleres', to: { name: 'workshops' } },
  { label: 'Parciales', to: { name: 'exams' } },
]

watch(
  () => route.query.q,
  (query) => {
    searchQuery.value = String(query ?? '')
  },
)

function closeMenu() {
  isMenuOpen.value = false
}

function submitSearch() {
  router.push({
    name: 'search',
    query: { q: searchQuery.value.trim() },
  })
  closeMenu()
}
</script>

<template>
  <header class="sticky top-0 z-20 border-b border-emerald-950/10 bg-white/95 shadow-sm backdrop-blur">
    <nav
      class="mx-auto flex max-w-6xl items-center justify-between gap-4 px-5 py-2 sm:px-6"
      aria-label="Navegación principal"
    >
      <RouterLink
        :to="{ name: 'home' }"
        class="flex shrink-0 items-center rounded-md focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-700"
        @click="closeMenu"
      >
        <img
          :src="logoMarkUrl"
          alt="Hidrología UdeA"
          class="h-10 w-auto sm:hidden"
          width="256"
          height="256"
        />
        <img
          :src="logoHeaderUrl"
          alt="Hidrología UdeA"
          class="hidden h-11 w-auto sm:block lg:h-12"
          width="768"
          height="300"
        />
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

          <form class="md:w-56 lg:w-64" role="search" @submit.prevent="submitSearch">
            <label for="site-search" class="sr-only">Buscar en Hidrología</label>
            <input
              id="site-search"
              v-model="searchQuery"
              type="search"
              name="q"
              maxlength="100"
              placeholder="Buscar en Hidrología..."
              class="w-full rounded-full border border-cyan-100 bg-cyan-50 px-4 py-2 text-sm font-bold text-slate-950 outline-none transition placeholder:text-slate-500 focus:border-emerald-700 focus:bg-white focus:ring-4 focus:ring-emerald-100"
            />
          </form>
        </div>
      </div>
    </nav>
  </header>
</template>
