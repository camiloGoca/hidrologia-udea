<script setup lang="ts">
import AppStatus from '@/components/AppStatus.vue'
</script>

<template>
  <main class="min-h-screen bg-slate-50 px-6 py-16 text-slate-950">
    <section class="mx-auto flex max-w-3xl flex-col gap-8">
      <div class="space-y-3">
        <p class="text-sm font-semibold uppercase tracking-wide text-sky-700">Frontend base</p>
        <h1 class="text-4xl font-bold">Hidrologia UdeA</h1>
        <p class="max-w-2xl text-lg text-slate-700">
          Aplicacion Vue inicial lista para continuar con el desarrollo incremental.
        </p>
      </div>

      <AppStatus />
    </section>
  </main>
</template>
