<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

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
  <main class="min-h-screen bg-slate-50 px-5 py-10 text-slate-950 sm:px-6">
    <section class="mx-auto max-w-5xl">
      <div class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200 sm:p-8">
        <p class="text-sm font-black uppercase text-emerald-700">Panel privado</p>
        <div class="mt-3 flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <h1 class="text-4xl font-black">Panel administrativo</h1>
            <p class="mt-3 max-w-2xl text-lg leading-8 text-slate-700">
              Sesión verificada. En una siguiente etapa agregaremos las herramientas de gestión de
              preguntas, publicaciones, hashtags y enlaces.
            </p>
          </div>

          <button
            type="button"
            class="rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950 disabled:cursor-not-allowed disabled:bg-slate-400"
            :disabled="isSigningOut"
            @click="logout"
          >
            {{ isSigningOut ? 'Cerrando sesión...' : 'Cerrar sesión' }}
          </button>
        </div>
      </div>
    </section>
  </main>
</template>
