<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import HashtagChip from '@/components/HashtagChip.vue'
import PageBanner from '@/components/PageBanner.vue'
import { searchPosts } from '@/services/api/postService'
import type { PostSearchResult } from '@/types/post'

const MIN_QUERY_LENGTH = 2
const MAX_QUERY_LENGTH = 100

const route = useRoute()
const results = ref<PostSearchResult[]>([])
const isLoading = ref(false)
const hasError = ref(false)
const searchedQuery = ref('')
let requestSequence = 0

const query = computed(() => String(route.query.q ?? '').trim())
const queryError = computed(() => {
  if (!query.value) {
    return 'Escribe un término para buscar publicaciones.'
  }
  if (query.value.length < MIN_QUERY_LENGTH) {
    return 'La búsqueda debe tener al menos 2 caracteres.'
  }
  if (query.value.length > MAX_QUERY_LENGTH) {
    return 'La búsqueda debe tener máximo 100 caracteres.'
  }

  return ''
})
const hasResults = computed(() => !isLoading.value && !hasError.value && results.value.length > 0)
const isEmpty = computed(
  () => !queryError.value && !isLoading.value && !hasError.value && results.value.length === 0,
)
const resultCountLabel = computed(() =>
  results.value.length === 1 ? '1 resultado' : `${results.value.length} resultados`,
)

watch(
  query,
  async (currentQuery) => {
    const currentRequest = ++requestSequence
    results.value = []
    hasError.value = false
    searchedQuery.value = currentQuery

    if (queryError.value) {
      isLoading.value = false
      return
    }

    isLoading.value = true
    try {
      const response = await searchPosts(currentQuery)
      if (currentRequest === requestSequence) {
        results.value = response
      }
    } catch {
      if (currentRequest === requestSequence) {
        hasError.value = true
      }
    } finally {
      if (currentRequest === requestSequence) {
        isLoading.value = false
      }
    }
  },
  { immediate: true },
)
</script>

<template>
  <div>
    <PageBanner
      eyebrow="Búsqueda"
      title="Resultados de búsqueda"
      description="Consulta publicaciones académicas por título, contenido o hashtags."
    />

    <section class="mx-auto max-w-6xl px-5 py-12 sm:px-6 sm:py-16">
      <RouterLink
        :to="{ name: 'home' }"
        class="rounded-md text-sm font-black text-sky-800 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
      >
        Volver al inicio
      </RouterLink>

      <div
        v-if="queryError"
        class="mt-8 rounded-3xl border border-amber-100 bg-amber-50 px-6 py-10 text-center"
        role="status"
      >
        <p class="text-lg font-black text-amber-950">{{ queryError }}</p>
        <p class="mt-2 text-sm leading-6 text-amber-800">
          Puedes buscar por título, texto de una publicación o hashtag.
        </p>
      </div>

      <div
        v-else-if="isLoading"
        class="mt-8 rounded-3xl border border-emerald-100 bg-emerald-50 px-6 py-10 text-center text-lg font-bold text-emerald-950"
        role="status"
      >
        Buscando publicaciones...
      </div>

      <div
        v-else-if="hasError"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
        role="alert"
      >
        <p class="text-lg font-black text-red-950">No pudimos completar la búsqueda.</p>
        <p class="mt-2 text-sm leading-6 text-red-800">Intenta nuevamente en unos momentos.</p>
      </div>

      <div
        v-else-if="isEmpty"
        class="relative mt-8 overflow-hidden rounded-3xl border border-cyan-100 bg-gradient-to-br from-cyan-50 to-white px-6 py-12 text-center shadow-sm sm:px-10"
      >
        <div
          class="absolute -right-14 -top-14 size-44 rounded-full border border-cyan-200"
          aria-hidden="true"
        />
        <p class="relative text-sm font-black uppercase text-cyan-800">Sin resultados</p>
        <h1 class="relative mt-3 text-3xl font-black text-slate-950">
          No encontramos publicaciones para "{{ searchedQuery }}".
        </h1>
      </div>

      <div v-else-if="hasResults" class="mt-8">
        <header class="max-w-3xl">
          <p class="text-sm font-black uppercase text-emerald-800">
            {{ resultCountLabel }} para "{{ searchedQuery }}"
          </p>
        </header>

        <div class="mt-6 grid gap-5">
          <article
            v-for="post in results"
            :key="post.id"
            class="rounded-3xl border border-cyan-100 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg sm:p-7"
          >
            <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
              <div class="min-w-0">
                <p class="text-sm font-black uppercase text-emerald-800">
                  {{ post.section.name }}
                </p>
                <h2 class="mt-3 break-words text-2xl font-black leading-tight text-slate-950">
                  {{ post.title }}
                </h2>
                <p class="mt-3 leading-7 text-slate-700">{{ post.snippet }}</p>

                <div v-if="post.tags.length > 0" class="mt-5 flex flex-wrap gap-2">
                  <HashtagChip v-for="tag in post.tags" :key="tag.slug" :tag="tag" />
                </div>
              </div>

              <RouterLink
                :to="{ name: 'post-detail', params: { id: post.id } }"
                class="shrink-0 rounded-2xl bg-sky-950 px-5 py-3 text-center text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950"
              >
                Leer publicación
              </RouterLink>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>
