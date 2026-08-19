<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import PostCard from '@/components/PostCard.vue'
import { getPostsByTag } from '@/services/api/postService'
import type { TagPostsResponse } from '@/types/post'

const route = useRoute()
const response = ref<TagPostsResponse | null>(null)
const isLoading = ref(true)
const hasError = ref(false)

const slug = computed(() => String(route.params.slug ?? ''))
const isEmpty = computed(
  () => !isLoading.value && !hasError.value && response.value?.posts.length === 0,
)

watch(
  slug,
  async (currentSlug) => {
    isLoading.value = true
    hasError.value = false
    response.value = null

    try {
      response.value = await getPostsByTag(currentSlug)
    } catch {
      hasError.value = true
    } finally {
      isLoading.value = false
    }
  },
  { immediate: true },
)
</script>

<template>
  <section class="mx-auto max-w-6xl px-5 py-12 sm:px-6 sm:py-16">
    <RouterLink
      :to="{ name: 'home' }"
      class="rounded-md text-sm font-black text-sky-800 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
    >
      Volver al inicio
    </RouterLink>

    <header class="mt-8 max-w-3xl">
      <p class="text-sm font-black uppercase text-emerald-800">Hashtag</p>
      <h1 class="mt-3 text-4xl font-black text-slate-950 sm:text-5xl">
        #{{ response?.tag.name ?? slug }}
      </h1>
      <p class="mt-4 leading-7 text-slate-700">Publicaciones relacionadas con este hashtag.</p>
    </header>

    <div
      v-if="isLoading"
      class="mt-8 rounded-3xl border border-emerald-100 bg-emerald-50 px-6 py-10 text-center text-lg font-bold text-emerald-950"
    >
      Cargando publicaciones...
    </div>

    <div
      v-else-if="hasError"
      class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
    >
      <p class="text-lg font-black text-red-950">No pudimos cargar este hashtag.</p>
      <p class="mt-2 text-sm leading-6 text-red-800">
        Puede que no exista o que no esté disponible para consulta pública.
      </p>
    </div>

    <div
      v-else-if="isEmpty"
      class="relative mt-8 overflow-hidden rounded-3xl border border-cyan-100 bg-gradient-to-br from-cyan-50 to-white px-6 py-12 text-center shadow-sm sm:px-10"
    >
      <div
        class="absolute -right-14 -top-14 size-44 rounded-full border border-cyan-200"
        aria-hidden="true"
      />
      <p class="relative text-sm font-black uppercase text-cyan-800">Sin publicaciones</p>
      <h2 class="relative mt-3 text-3xl font-black text-slate-950">
        Aún no hay publicaciones asociadas a este hashtag.
      </h2>
    </div>

    <div v-else-if="response" class="mt-8 grid gap-6 md:grid-cols-2">
      <PostCard v-for="post in response.posts" :key="post.id" :post="post" />
    </div>
  </section>
</template>
