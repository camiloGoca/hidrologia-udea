<script setup lang="ts">
import udeaLogoUrl from '@/assets/brand/logo-udea.png'

defineProps<{
  siteVisits?: number | null
}>()
</script>

<template>
  <footer class="bg-slate-950 text-white">
    <div class="relative overflow-hidden border-t border-cyan-400/20">
      <svg
        class="absolute inset-x-0 top-0 h-16 w-full text-slate-50"
        viewBox="0 0 1440 80"
        preserveAspectRatio="none"
        aria-hidden="true"
      >
        <path
          d="M0 0H1440V28C1320 50 1200 62 1080 60C960 58 840 42 720 34C600 26 480 26 360 40C240 54 120 82 0 64V0Z"
          fill="currentColor"
        />
      </svg>

      <div class="mx-auto grid max-w-6xl gap-10 px-5 pb-10 pt-24 sm:px-6 lg:grid-cols-[0.8fr_1fr_1fr]">
        <div>
          <img
            :src="udeaLogoUrl"
            alt="Logo de la Universidad de Antioquia"
            class="h-auto w-44 max-w-full"
          />
        </div>

        <div>
          <p class="text-xl font-black">Hidrología UdeA</p>
          <p class="mt-4 leading-7 text-slate-300">Facultad de Ingeniería</p>
          <p class="leading-7 text-slate-300">Universidad de Antioquia</p>
        </div>

        <div>
          <p class="text-sm font-bold uppercase text-cyan-200">Identidad académica</p>
          <p class="mt-4 max-w-sm leading-7 text-slate-300">
            Un espacio de consulta para estudiar el agua, las cuencas y los procesos hidrológicos
            desde una mirada académica.
          </p>
        </div>
      </div>

      <div class="border-t border-white/10">
        <div
          class="mx-auto flex max-w-6xl flex-col gap-2 px-5 py-4 text-sm text-slate-400 sm:flex-row sm:items-center sm:justify-between sm:px-6"
        >
          <span>© Hidrología UdeA</span>
          <span v-if="typeof siteVisits === 'number'" class="font-bold text-cyan-100">
            Visitas al sitio: {{ siteVisits }}
          </span>
        </div>
      </div>
    </div>
  </footer>
</template>
