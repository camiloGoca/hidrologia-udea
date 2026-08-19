<script setup lang="ts">
import { onMounted, ref } from 'vue'

import PageBanner from '@/components/PageBanner.vue'
import { getInterestingLinks } from '@/services/api/linkService'
import type { InterestingLink } from '@/types/interestingLink'

const links = ref<InterestingLink[]>([])
const isLoading = ref(true)
const hasError = ref(false)

onMounted(async () => {
  try {
    links.value = await getInterestingLinks()
  } catch {
    hasError.value = true
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <div>
    <PageBanner
      eyebrow="Recursos"
      title="Enlaces de interés"
      description="Recursos generales seleccionados por el profesor para acompañar el estudio de Hidrología."
    />

    <section class="mx-auto max-w-6xl px-5 py-12 sm:px-6 sm:py-16">
      <div
        v-if="isLoading"
        class="rounded-3xl border border-emerald-100 bg-emerald-50 px-6 py-10 text-center text-lg font-bold text-emerald-950"
      >
        Cargando recursos...
      </div>

      <div
        v-else-if="hasError"
        class="rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
      >
        <p class="text-lg font-black text-red-950">No pudimos cargar los enlaces de interés.</p>
        <p class="mt-2 text-sm leading-6 text-red-800">
          Intenta nuevamente en unos minutos. Si el problema continúa, revisaremos la conexión con
          la API.
        </p>
      </div>

      <div
        v-else-if="links.length === 0"
        class="relative overflow-hidden rounded-3xl border border-cyan-100 bg-gradient-to-br from-cyan-50 to-white px-6 py-12 text-center shadow-sm sm:px-10"
      >
        <div
          class="absolute -right-14 -top-14 size-44 rounded-full border border-cyan-200"
          aria-hidden="true"
        />
        <p class="relative text-sm font-black uppercase text-cyan-800">Biblioteca en preparación</p>
        <h2 class="relative mt-3 text-3xl font-black text-slate-950">
          Aún no hay enlaces de interés publicados.
        </h2>
        <p class="relative mx-auto mt-4 max-w-2xl leading-7 text-slate-700">
          Cuando el profesor apruebe recursos externos, aparecerán aquí ordenados para consulta
          pública.
        </p>
      </div>

      <ul v-else class="grid gap-6 md:grid-cols-2">
        <li
          v-for="link in links"
          :key="link.id"
          class="group relative overflow-hidden rounded-3xl bg-gradient-to-br from-emerald-800 to-sky-950 p-6 text-white shadow-lg transition duration-200 hover:-translate-y-1 hover:shadow-xl sm:p-7"
        >
          <div
            class="absolute -right-14 -top-14 size-44 rounded-full border border-white/15"
            aria-hidden="true"
          />

          <div class="relative flex min-h-56 flex-col justify-between">
            <div>
              <p class="text-xs font-black uppercase text-emerald-100">
                Recurso {{ link.displayOrder }}
              </p>
              <h2 class="mt-3 text-2xl font-black">{{ link.title }}</h2>
              <p v-if="link.description" class="mt-4 leading-7 text-white/85">
                {{ link.description }}
              </p>
            </div>

            <a
              :href="link.url"
              target="_blank"
              rel="noopener noreferrer"
              class="mt-8 inline-flex w-fit items-center rounded-full bg-white px-5 py-3 text-sm font-black text-sky-950 shadow-sm transition hover:bg-emerald-50 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
              :aria-label="`Visitar recurso: ${link.title}`"
            >
              Visitar recurso
              <span class="ml-2 transition group-hover:translate-x-1" aria-hidden="true">-&gt;</span>
            </a>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>
