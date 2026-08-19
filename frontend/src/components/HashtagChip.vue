<script setup lang="ts">
import type { PostTag } from '@/types/post'

defineProps<{
  tag: PostTag
}>()
</script>

<template>
  <RouterLink
    :to="{ name: 'hashtag-detail', params: { slug: tag.slug } }"
    class="inline-flex items-center rounded-full bg-cyan-50 px-3 py-1 text-xs font-black text-cyan-900 ring-1 ring-cyan-100 transition hover:bg-cyan-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-cyan-700"
  >
    #{{ tag.name }}
  </RouterLink>
</template>
