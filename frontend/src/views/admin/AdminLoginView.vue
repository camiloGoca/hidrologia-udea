<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import loginImageUrl from '@/assets/images/login.jpg'
import { getAdminMe } from '@/services/api/adminService'
import { signIn, signOut } from '@/services/firebase/authService'

const router = useRouter()
const route = useRoute()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')

const isForbiddenRedirect = computed(() => route.query.reason === 'forbidden')
const passwordInputType = computed(() => (showPassword.value ? 'text' : 'password'))

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
    errorMessage.value = 'No fue posible iniciar sesión. Verifica el correo y la contraseña.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="min-h-screen bg-slate-950 text-white">
    <section class="relative isolate flex min-h-screen items-center overflow-hidden px-5 py-10 sm:px-6">
      <div
        class="absolute inset-0 -z-10 bg-cover bg-center"
        :style="{ backgroundImage: `linear-gradient(90deg, rgba(2, 44, 34, 0.92), rgba(8, 47, 73, 0.68), rgba(15, 23, 42, 0.38)), url(${loginImageUrl})` }"
        aria-hidden="true"
      />
      <div class="absolute inset-0 -z-10 bg-slate-950/20" aria-hidden="true" />

      <div class="mx-auto grid w-full max-w-6xl items-center gap-10 lg:grid-cols-[1fr_27rem]">
        <div class="max-w-2xl">
          <RouterLink
            :to="{ name: 'home' }"
            class="inline-flex rounded-full bg-white/10 px-4 py-2 text-sm font-black uppercase tracking-[0.16em] text-emerald-50 ring-1 ring-white/20 backdrop-blur transition hover:bg-white/20 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
          >
            Hidrología UdeA
          </RouterLink>
          <h1 class="mt-6 text-4xl font-black leading-tight sm:text-5xl">
            Administración académica del portal
          </h1>
          <p class="mt-5 max-w-xl text-lg leading-8 text-white/82">
            Acceso privado del profesor para revisar preguntas, preparar publicaciones, administrar
            enlaces y consultar estadísticas del curso.
          </p>
        </div>

        <form
          class="rounded-[2rem] bg-white/96 p-6 text-slate-950 shadow-2xl ring-1 ring-white/40 backdrop-blur sm:p-8"
          aria-label="Iniciar sesión como administrador"
          @submit.prevent="submitLogin"
        >
          <div>
            <p class="text-sm font-black uppercase text-emerald-700">Acceso privado</p>
            <h2 class="mt-2 text-3xl font-black">Iniciar sesión</h2>
            <p class="mt-3 text-sm leading-6 text-slate-600">
              Usa la cuenta autorizada para administrar Hidrología UdeA.
            </p>
          </div>

          <p
            v-if="isForbiddenRedirect"
            class="mt-5 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-950"
            role="status"
          >
            Esta cuenta no está autorizada para administrar el portal.
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
              <div class="mt-2 flex rounded-2xl border border-slate-300 bg-white shadow-sm transition focus-within:border-emerald-700 focus-within:ring-4 focus-within:ring-emerald-100">
                <input
                  id="admin-password"
                  v-model="password"
                  :type="passwordInputType"
                  autocomplete="current-password"
                  required
                  class="min-w-0 flex-1 rounded-2xl border-0 px-4 py-3 text-base font-semibold text-slate-950 outline-none"
                  :aria-describedby="errorMessage ? 'login-error' : undefined"
                />
                <button
                  type="button"
                  class="shrink-0 rounded-2xl px-4 text-sm font-black text-emerald-800 transition hover:bg-emerald-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-800"
                  :aria-pressed="showPassword"
                  @click="showPassword = !showPassword"
                >
                  {{ showPassword ? 'Ocultar' : 'Ver' }}
                </button>
              </div>
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
      </div>
    </section>
  </main>
</template>
