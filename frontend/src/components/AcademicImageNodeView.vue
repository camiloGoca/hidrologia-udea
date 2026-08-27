<script setup lang="ts">
import { computed } from 'vue'
import type { Node as ProseMirrorNode } from '@tiptap/pm/model'
import { NodeViewWrapper } from '@tiptap/vue-3'

interface EditorImageMetadata {
  id: number
  secureUrl: string
  width: number
  height: number
  altText: string
}

interface ImageExtensionOptions {
  getImageById?: (id: number) => EditorImageMetadata | undefined
  onEditImage?: (id: number, position: number) => void
}

const props = defineProps<{
  node: ProseMirrorNode
  selected: boolean
  getPos: () => number
  extension: {
    options: ImageExtensionOptions
  }
}>()

const postImageId = computed(() => Number(props.node.attrs.postImageId))
const caption = computed(() => props.node.attrs.caption as string | null | undefined)
const displaySize = computed(() => {
  const value = props.node.attrs.displaySize

  return value === 'small' || value === 'large' ? value : 'medium'
})
const image = computed(() => props.extension.options.getImageById?.(postImageId.value))

function editImage() {
  props.extension.options.onEditImage?.(postImageId.value, props.getPos())
}
</script>

<template>
  <NodeViewWrapper
    as="figure"
    class="editor-image-node"
    :class="{ 'editor-image-node-selected': selected }"
    :data-display-size="displaySize"
    data-drag-handle
  >
    <div v-if="image" class="editor-image-frame">
      <img
        :src="image.secureUrl"
        :alt="image.altText"
        :width="image.width"
        :height="image.height"
        draggable="false"
      />
    </div>
    <div v-else class="editor-image-missing" role="note">
      Imagen no disponible
    </div>
    <figcaption v-if="caption" class="editor-image-caption">
      {{ caption }}
    </figcaption>
    <button
      v-if="selected"
      type="button"
      class="editor-image-edit-button"
      @mousedown.prevent
      @click="editImage"
    >
      Editar imagen
    </button>
  </NodeViewWrapper>
</template>
