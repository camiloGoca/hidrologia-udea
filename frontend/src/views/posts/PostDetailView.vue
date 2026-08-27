<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import HashtagChip from '@/components/HashtagChip.vue'
import PostContentRenderer from '@/components/PostContentRenderer'
import { getPostById } from '@/services/api/postService'
import type { PostDetail } from '@/types/post'

const route = useRoute()
const post = ref<PostDetail | null>(null)
const isLoading = ref(true)
const hasError = ref(false)

const postId = computed(() => String(route.params.id ?? ''))
const sectionRoute = computed(() => {
  if (!post.value) {
    return { name: 'home' }
  }

  return {
    name: post.value.section.type === 'TALLER' ? 'workshop-detail' : 'exam-detail',
    params: { slug: post.value.section.slug },
  }
})
const sectionListRoute = computed(() =>
  post.value?.section.type === 'TALLER' ? 'workshops' : 'exams',
)
const formattedDate = computed(() =>
  post.value
    ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' }).format(new Date(post.value.publishedAt))
    : '',
)

watch(
  postId,
  async (currentId) => {
    isLoading.value = true
    hasError.value = false
    post.value = null

    try {
      post.value = await getPostById(currentId)
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
  <section class="mx-auto max-w-4xl px-5 py-12 sm:px-6 sm:py-16">
    <RouterLink
      :to="{ name: sectionListRoute }"
      class="rounded-md text-sm font-black text-sky-800 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
    >
      Volver
    </RouterLink>

    <div
      v-if="isLoading"
      class="mt-8 rounded-3xl border border-emerald-100 bg-emerald-50 px-6 py-10 text-center text-lg font-bold text-emerald-950"
    >
      Cargando publicación...
    </div>

    <div
      v-else-if="hasError"
      class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
    >
      <p class="text-lg font-black text-red-950">No pudimos cargar esta publicación.</p>
      <p class="mt-2 text-sm leading-6 text-red-800">
        Puede que no exista o que todavía no esté publicada.
      </p>
    </div>

    <article v-else-if="post" class="mt-8">
      <header class="rounded-3xl bg-sky-950 p-7 text-white shadow-lg sm:p-10">
        <RouterLink
          :to="sectionRoute"
          class="inline-flex rounded-full bg-white/10 px-4 py-2 text-sm font-black text-emerald-100 transition hover:bg-white/15 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
        >
          {{ post.section.name }}
        </RouterLink>
        <h1 class="mt-6 text-4xl font-black leading-tight sm:text-5xl">{{ post.title }}</h1>
        <p class="mt-4 text-sm font-semibold uppercase text-cyan-100">{{ formattedDate }}</p>
      </header>

      <div v-if="post.tags.length > 0" class="mt-6 flex flex-wrap gap-2">
        <HashtagChip v-for="tag in post.tags" :key="tag.slug" :tag="tag" />
      </div>

      <PostContentRenderer class="mt-8" :document="post.contentDocument" :images="post.images" />
    </article>
  </section>
</template>
