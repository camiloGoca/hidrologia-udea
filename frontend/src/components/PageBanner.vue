<script setup lang="ts">
import rioImageUrl from '@/assets/images/rio-hidrologia.jpg'

defineProps<{
  eyebrow: string
  title: string
  description: string
}>()
</script>

<template>
  <section
    class="page-banner relative isolate overflow-hidden bg-sky-950 text-white"
    :style="{ '--banner-image': `url(${rioImageUrl})` }"
  >
    <svg
      class="absolute inset-x-0 bottom-0 h-20 w-full text-slate-50 sm:h-24"
      viewBox="0 0 1440 120"
      preserveAspectRatio="none"
      aria-hidden="true"
    >
      <path
        d="M0 84L80 72C160 60 320 36 480 42C640 48 800 84 960 90C1120 96 1280 72 1360 60L1440 48V120H0V84Z"
        fill="currentColor"
      />
    </svg>

    <div class="relative mx-auto max-w-6xl px-5 pb-20 pt-10 sm:px-6 sm:pb-28 sm:pt-16">
      <nav class="text-sm font-semibold text-cyan-50/95" aria-label="Breadcrumb">
        <RouterLink
          :to="{ name: 'home' }"
          class="rounded-md underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
        >
          Inicio
        </RouterLink>
        <span class="mx-2" aria-hidden="true">/</span>
        <span>{{ title }}</span>
      </nav>

      <div class="mt-8 max-w-3xl sm:mt-10">
        <p class="text-sm font-bold uppercase text-emerald-100">{{ eyebrow }}</p>
        <h1 class="mt-3 text-4xl font-black sm:text-6xl">{{ title }}</h1>
        <p class="mt-5 max-w-2xl text-lg leading-8 text-sky-50">
          {{ description }}
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page-banner {
  background-image:
    linear-gradient(90deg, rgba(2, 44, 34, 0.86) 0%, rgba(8, 47, 73, 0.52) 45%, rgba(15, 23, 42, 0.14) 100%),
    var(--banner-image);
  background-position: center 46%;
  background-size: cover;
}

@media (max-width: 640px) {
  .page-banner {
    background-image:
      linear-gradient(90deg, rgba(2, 44, 34, 0.84) 0%, rgba(8, 47, 73, 0.58) 62%, rgba(15, 23, 42, 0.22) 100%),
      var(--banner-image);
    background-position: 55% center;
  }
}
</style>
