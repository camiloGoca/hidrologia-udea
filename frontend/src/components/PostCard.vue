<script setup lang="ts">
import { computed } from 'vue'
import type { RouteLocationRaw } from 'vue-router'

import HashtagChip from '@/components/HashtagChip.vue'
import type { PostSummary } from '@/types/post'

const props = defineProps<{
  post: PostSummary
}>()

const detailRoute = computed<RouteLocationRaw>(() => ({
  name: 'post-detail',
  params: { id: props.post.id },
}))

const formattedDate = computed(() =>
  new Intl.DateTimeFormat('es-CO', {
    dateStyle: 'medium',
  }).format(new Date(props.post.publishedAt)),
)
</script>

<template>
  <article
    class="group flex min-h-64 flex-col rounded-3xl border border-cyan-100 bg-white p-6 shadow-sm transition duration-200 hover:-translate-y-1 hover:shadow-xl sm:p-7"
  >
    <div class="flex items-start justify-between gap-4">
      <p class="text-sm font-black uppercase text-emerald-800">{{ formattedDate }}</p>
      <span
        class="grid size-10 shrink-0 place-items-center rounded-full bg-sky-950 text-lg font-black text-white transition group-hover:translate-x-1"
        aria-hidden="true"
      >
        -&gt;
      </span>
    </div>

    <h2 class="mt-4 text-2xl font-black leading-tight text-slate-950">
      {{ post.title }}
    </h2>

    <div v-if="post.tags.length > 0" class="mt-5 flex flex-wrap gap-2">
      <HashtagChip v-for="tag in post.tags" :key="tag.slug" :tag="tag" />
    </div>

    <RouterLink
      :to="detailRoute"
      class="mt-auto pt-8 text-sm font-black uppercase text-sky-900 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
      :aria-label="`Leer publicacion: ${post.title}`"
    >
      Leer publicación
    </RouterLink>
  </article>
</template>
