<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'

import { signOut } from '@/services/firebase/authService'

const router = useRouter()
const isSigningOut = ref(false)

async function logout() {
  if (isSigningOut.value) {
    return
  }

  isSigningOut.value = true
  await signOut()
  await router.push({ name: 'admin-login' })
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-950">
    <header class="border-b border-slate-200 bg-white">
      <div
        class="mx-auto flex max-w-6xl flex-col gap-4 px-5 py-5 sm:px-6 lg:flex-row lg:items-center lg:justify-between"
      >
        <div>
          <p class="text-sm font-black uppercase text-emerald-700">Hidrología UdeA</p>
          <h1 class="mt-1 text-2xl font-black">Panel administrativo</h1>
        </div>

        <nav
          class="flex flex-wrap items-center gap-2 text-sm font-black"
          aria-label="Navegación administrativa"
        >
          <RouterLink
            :to="{ name: 'admin-home' }"
            class="rounded-2xl px-4 py-2 text-slate-700 transition hover:bg-emerald-50 hover:text-emerald-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-800"
          >
            Inicio
          </RouterLink>
          <RouterLink
            :to="{ name: 'admin-questions' }"
            class="rounded-2xl px-4 py-2 text-slate-700 transition hover:bg-emerald-50 hover:text-emerald-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-800"
          >
            Preguntas
          </RouterLink>
          <RouterLink
            :to="{ name: 'admin-posts' }"
            class="rounded-2xl px-4 py-2 text-slate-700 transition hover:bg-emerald-50 hover:text-emerald-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-800"
          >
            Publicaciones
          </RouterLink>
          <RouterLink
            :to="{ name: 'admin-hashtags' }"
            class="rounded-2xl px-4 py-2 text-slate-700 transition hover:bg-emerald-50 hover:text-emerald-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-800"
          >
            Hashtags
          </RouterLink>
          <RouterLink
            :to="{ name: 'admin-links' }"
            class="rounded-2xl px-4 py-2 text-slate-700 transition hover:bg-emerald-50 hover:text-emerald-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-800"
          >
            Enlaces
          </RouterLink>
          <button
            type="button"
            class="rounded-2xl bg-sky-950 px-4 py-2 text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-950 disabled:cursor-not-allowed disabled:bg-slate-400"
            :disabled="isSigningOut"
            @click="logout"
          >
            {{ isSigningOut ? 'Cerrando sesión...' : 'Cerrar sesión' }}
          </button>
        </nav>
      </div>
    </header>

    <RouterView />
  </div>
</template>
