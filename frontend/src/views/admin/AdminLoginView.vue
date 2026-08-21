<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getAdminMe } from '@/services/api/adminService'
import { signIn, signOut } from '@/services/firebase/authService'

const router = useRouter()
const route = useRoute()

const email = ref('')
const password = ref('')
const isSubmitting = ref(false)
const errorMessage = ref('')

const isForbiddenRedirect = computed(() => route.query.reason === 'forbidden')

async function submitLogin() {
  if (isSubmitting.value) {
    return
  }

  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await signIn(email.value.trim(), password.value)
    await getAdminMe()
    await router.push({ name: 'admin-home' })
  } catch {
    await signOut().catch(() => undefined)
    errorMessage.value = 'No fue posible iniciar sesión. Verifica tus credenciales.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main
    class="min-h-screen bg-gradient-to-br from-emerald-950 via-sky-950 to-slate-950 px-5 py-10 text-white sm:px-6"
  >
    <section class="mx-auto grid min-h-[calc(100vh-5rem)] max-w-6xl items-center gap-10 lg:grid-cols-[1fr_28rem]">
      <div class="max-w-2xl">
        <p class="text-sm font-black uppercase tracking-[0.18em] text-emerald-200">
          Hidrología UdeA
        </p>
        <h1 class="mt-4 text-4xl font-black leading-tight sm:text-5xl">
          Administración del contenido académico
        </h1>
        <p class="mt-5 max-w-xl text-lg leading-8 text-white/78">
          Ingreso privado para el profesor. La autorización final se valida en el backend antes de
          permitir el acceso al panel.
        </p>
      </div>

      <form
        class="rounded-[2rem] bg-white p-6 text-slate-950 shadow-2xl sm:p-8"
        aria-label="Iniciar sesión como administrador"
        @submit.prevent="submitLogin"
      >
        <div>
          <p class="text-sm font-black uppercase text-emerald-700">Acceso privado</p>
          <h2 class="mt-2 text-3xl font-black">Iniciar sesión</h2>
        </div>

        <p
          v-if="isForbiddenRedirect"
          class="mt-5 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-950"
          role="status"
        >
          Tu cuenta de Firebase no está autorizada como administrador.
        </p>

        <p
          v-if="errorMessage"
          id="login-error"
          class="mt-5 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-950"
          role="alert"
        >
          {{ errorMessage }}
        </p>

        <div class="mt-6 space-y-5">
          <div>
            <label for="admin-email" class="block text-sm font-black text-slate-800">Correo</label>
            <input
              id="admin-email"
              v-model="email"
              type="email"
              autocomplete="email"
              required
              class="mt-2 w-full rounded-2xl border border-slate-300 px-4 py-3 text-base font-semibold text-slate-950 shadow-sm outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
              :aria-describedby="errorMessage ? 'login-error' : undefined"
            />
          </div>

          <div>
            <label for="admin-password" class="block text-sm font-black text-slate-800">
              Contraseña
            </label>
            <input
              id="admin-password"
              v-model="password"
              type="password"
              autocomplete="current-password"
              required
              class="mt-2 w-full rounded-2xl border border-slate-300 px-4 py-3 text-base font-semibold text-slate-950 shadow-sm outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
              :aria-describedby="errorMessage ? 'login-error' : undefined"
            />
          </div>
        </div>

        <button
          type="submit"
          class="mt-7 w-full rounded-2xl bg-emerald-800 px-5 py-3 text-base font-black text-white shadow-lg transition hover:bg-emerald-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800 disabled:cursor-not-allowed disabled:bg-slate-400"
          :disabled="isSubmitting"
        >
          {{ isSubmitting ? 'Iniciando sesión...' : 'Iniciar sesión' }}
        </button>
      </form>
    </section>
  </main>
</template>
