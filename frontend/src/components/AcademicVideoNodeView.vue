<script setup lang="ts">
import { computed } from 'vue'
import type { Node as ProseMirrorNode } from '@tiptap/pm/model'
import { NodeViewWrapper } from '@tiptap/vue-3'

import type { PostContentVideoProvider } from '@/types/postContent'

const props = defineProps<{
  node: ProseMirrorNode
  selected: boolean
}>()

const provider = computed(() => props.node.attrs.provider as PostContentVideoProvider)
const sourceUrl = computed(() => props.node.attrs.sourceUrl as string)
const videoId = computed(() => props.node.attrs.videoId as string | null | undefined)
const label = computed(() => {
  switch (provider.value) {
    case 'youtube':
      return 'Video de YouTube'
    case 'tiktok':
      return 'Video de TikTok'
    case 'direct':
      return 'Video externo'
    default:
      return 'Video'
  }
})
const sourceHost = computed(() => {
  try {
    return new URL(sourceUrl.value).hostname
  } catch {
    return sourceUrl.value
  }
})
</script>

<template>
  <NodeViewWrapper
    as="figure"
    class="editor-video-node"
    :class="{ 'editor-video-node-selected': selected }"
    :data-provider="provider"
    data-drag-handle
  >
    <div class="editor-video-icon" aria-hidden="true">Video</div>
    <figcaption class="editor-video-caption">
      <span class="editor-video-title">{{ label }}</span>
      <span class="editor-video-source">{{ sourceHost }}</span>
      <span v-if="videoId" class="editor-video-id">ID {{ videoId }}</span>
    </figcaption>
  </NodeViewWrapper>
</template>
