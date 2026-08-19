<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import PageBanner from '@/components/PageBanner.vue'
import PostCard from '@/components/PostCard.vue'
import { getPostsBySection } from '@/services/api/postService'
import type { SectionPostsResponse } from '@/types/post'
import type { SectionType } from '@/types/section'

const props = defineProps<{
  sectionKind: 'taller' | 'parcial'
}>()

const route = useRoute()
const response = ref<SectionPostsResponse | null>(null)
const isLoading = ref(true)
const hasError = ref(false)

const expectedSectionType = computed<SectionType>(() =>
  props.sectionKind === 'taller' ? 'TALLER' : 'PARCIAL',
)
const sectionLabel = computed(() => (props.sectionKind === 'taller' ? 'Talleres' : 'Parciales'))
const backRouteName = computed(() => (props.sectionKind === 'taller' ? 'workshops' : 'exams'))
const slug = computed(() => String(route.params.slug ?? ''))
const validResponse = computed(() =>
  response.value?.section.type === expectedSectionType.value ? response.value : null,
)
const hasTypeMismatch = computed(
  () => response.value !== null && response.value.section.type !== expectedSectionType.value,
)
const isEmpty = computed(
  () => !isLoading.value && !hasError.value && validResponse.value?.posts.length === 0,
)
const bannerTitle = computed(() => validResponse.value?.section.name ?? 'Publicaciones')
const bannerDescription = computed(
  () =>
    validResponse.value?.section.description ??
    'Consulta las publicaciones disponibles para esta sección.',
)

watch(
  slug,
  async (currentSlug) => {
    isLoading.value = true
    hasError.value = false
    response.value = null

    try {
      response.value = await getPostsBySection(currentSlug)
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
  <div>
    <PageBanner
      :eyebrow="sectionKind === 'taller' ? 'Taller' : 'Parcial'"
      :title="bannerTitle"
      :description="bannerDescription"
    />

    <section class="mx-auto max-w-6xl px-5 py-12 sm:px-6 sm:py-16">
      <RouterLink
        :to="{ name: backRouteName }"
        class="rounded-md text-sm font-black text-sky-800 underline-offset-4 hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-700"
      >
        Volver a {{ sectionLabel }}
      </RouterLink>

      <div
        v-if="isLoading"
        class="mt-8 rounded-3xl border border-emerald-100 bg-emerald-50 px-6 py-10 text-center text-lg font-bold text-emerald-950"
      >
        Cargando publicaciones...
      </div>

      <div
        v-else-if="hasError || hasTypeMismatch"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
      >
        <p class="text-lg font-black text-red-950">
          No pudimos cargar las publicaciones de esta sección.
        </p>
        <p class="mt-2 text-sm leading-6 text-red-800">
          Verifica la sección seleccionada o intenta nuevamente en unos momentos.
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
        <p class="relative text-sm font-black uppercase text-cyan-800">Contenido en preparación</p>
        <h1 class="relative mt-3 text-3xl font-black text-slate-950">
          Aún no hay publicaciones disponibles en esta sección.
        </h1>
        <p class="relative mx-auto mt-4 max-w-2xl leading-7 text-slate-700">
          Cuando el profesor publique soluciones o materiales asociados, aparecerán aquí.
        </p>
      </div>

      <div v-else-if="validResponse" class="mt-8 grid gap-6 md:grid-cols-2">
        <PostCard v-for="post in validResponse.posts" :key="post.id" :post="post" />
      </div>
    </section>
  </div>
</template>
