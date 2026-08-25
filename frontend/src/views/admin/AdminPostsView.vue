<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import { createAdminPost, getPostsByStatus } from '@/services/api/adminPostService'
import { getSections } from '@/services/api/sectionService'
import { signOut } from '@/services/firebase/authService'
import type { AdminPostStatus, AdminPostsResponse } from '@/types/adminPost'
import type { Section, SectionType } from '@/types/section'
import { adminPostStatusLabel } from '@/utils/adminPostStatus'

const PAGE_SIZE = 20

const POST_TABS = [
  {
    query: 'borradores',
    label: 'Borradores',
    status: 'DRAFT',
    emptyMessage: 'No hay borradores.',
    loadingMessage: 'Cargando borradores...',
  },
  {
    query: 'publicadas',
    label: 'Publicadas',
    status: 'PUBLISHED',
    emptyMessage: 'No hay publicaciones publicadas.',
    loadingMessage: 'Cargando publicaciones publicadas...',
  },
  {
    query: 'archivadas',
    label: 'Archivadas',
    status: 'ARCHIVED',
    emptyMessage: 'No hay publicaciones archivadas.',
    loadingMessage: 'Cargando publicaciones archivadas...',
  },
] as const

type PostTab = (typeof POST_TABS)[number]

const route = useRoute()
const router = useRouter()
const response = ref<AdminPostsResponse | null>(null)
const sections = ref<Section[]>([])
const isLoading = ref(true)
const hasError = ref(false)
const isCreateOpen = ref(false)
const isLoadingSections = ref(false)
const isCreating = ref(false)
const createSectionSlug = ref('')
const createError = ref('')
const createCancelButton = ref<HTMLButtonElement | null>(null)
let lastFocusedElement: HTMLElement | null = null

const activeTab = computed(() => findTab(route.query.estado) ?? POST_TABS[0])
const items = computed(() => response.value?.items ?? [])
const currentPage = computed(() => response.value?.page ?? 0)
const totalPages = computed(() => response.value?.totalPages ?? 0)
const hasMultiplePages = computed(() => totalPages.value > 1)
const isEmpty = computed(() => !isLoading.value && !hasError.value && items.value.length === 0)
const workshopSections = computed(() => sectionsByType('TALLER'))
const examSections = computed(() => sectionsByType('PARCIAL'))

watch(
  () => route.query.estado,
  (estado) => {
    if (estado && !findTab(estado)) {
      void router.replace({ name: 'admin-posts', query: { estado: 'borradores' } })
      return
    }

    void loadPosts(0)
  },
  { immediate: true },
)

async function loadPosts(page: number) {
  isLoading.value = true
  hasError.value = false

  try {
    response.value = await getPostsByStatus(activeTab.value.status as AdminPostStatus, page, PAGE_SIZE)
  } catch (error) {
    await handleAdminError(error, () => {
      hasError.value = true
      response.value = null
    })
  } finally {
    isLoading.value = false
  }
}

async function openCreateModal() {
  createError.value = ''
  isCreateOpen.value = true
  lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null
  await nextTick()
  createCancelButton.value?.focus()

  if (!sections.value.length) {
    await loadSections()
  }
}

function closeCreateModal() {
  if (isCreating.value) {
    return
  }

  isCreateOpen.value = false
  createError.value = ''
  lastFocusedElement?.focus()
}

async function loadSections() {
  isLoadingSections.value = true
  createError.value = ''

  try {
    sections.value = await getSections()
    if (!createSectionSlug.value && sections.value[0]) {
      createSectionSlug.value = sections.value[0].slug
    }
  } catch (error) {
    await handleAdminError(error, () => {
      createError.value = 'No pudimos cargar las secciones. Intenta nuevamente.'
    })
  } finally {
    isLoadingSections.value = false
  }
}

async function createPost() {
  if (isCreating.value) {
    return
  }

  if (!createSectionSlug.value) {
    createError.value = 'Selecciona una sección para crear el borrador.'
    return
  }

  isCreating.value = true
  createError.value = ''

  try {
    const createdPost = await createAdminPost({ sectionSlug: createSectionSlug.value })
    await router.push({ name: 'admin-post-detail', params: { id: createdPost.id } })
  } catch (error) {
    await handleAdminError(error, () => {
      createError.value = 'No pudimos crear el borrador. Intenta nuevamente.'
    })
  } finally {
    isCreating.value = false
  }
}

async function handleAdminError(error: unknown, fallback: () => void) {
  if (isAdminAuthorizationError(error)) {
    await signOut().catch(() => undefined)
    await router.push({ name: 'admin-login', query: { reason: 'forbidden' } })
    return
  }

  fallback()
}

function previousPage() {
  if (currentPage.value > 0) {
    void loadPosts(currentPage.value - 1)
  }
}

function nextPage() {
  if (currentPage.value + 1 < totalPages.value) {
    void loadPosts(currentPage.value + 1)
  }
}

function sectionsByType(type: SectionType) {
  return sections.value.filter((section) => section.type === type)
}

function findTab(value: unknown): PostTab | undefined {
  if (typeof value !== 'string') {
    return undefined
  }

  return POST_TABS.find((tab) => tab.query === value)
}

function displayTitle(title: string): string {
  return title.trim() || 'Sin título'
}

function relevantDateLabel(status: AdminPostStatus): string {
  if (status === 'PUBLISHED') {
    return 'Publicado'
  }

  if (status === 'ARCHIVED') {
    return 'Archivado'
  }

  return 'Actualizado'
}

function relevantDate(post: { status: AdminPostStatus; updatedAt: string; publishedAt: string | null }): string {
  if (post.status === 'PUBLISHED' && post.publishedAt) {
    return post.publishedAt
  }

  return post.updatedAt
}

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('es-CO', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
</script>

<template>
  <main class="px-5 py-10 sm:px-6">
    <section class="mx-auto max-w-6xl">
      <header class="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <p class="text-sm font-black uppercase text-emerald-700">Publicaciones</p>
          <h1 class="mt-3 text-4xl font-black">Publicaciones administradas</h1>
          <p class="mt-4 text-lg leading-8 text-slate-700">
            Consulta borradores, publicaciones visibles y archivadas, o crea un borrador manual
            cuando el contenido no nace de una pregunta estudiantil.
          </p>
        </div>

        <button
          type="button"
          class="inline-flex w-fit rounded-2xl bg-emerald-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
          @click="openCreateModal"
        >
          Nueva publicación
        </button>
      </header>

      <nav class="mt-8 flex flex-wrap gap-3" aria-label="Estado de publicaciones">
        <RouterLink
          v-for="tab in POST_TABS"
          :key="tab.query"
          :to="{ name: 'admin-posts', query: { estado: tab.query } }"
          class="rounded-2xl px-5 py-3 text-sm font-black transition focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
          :class="
            activeTab.query === tab.query
              ? 'bg-emerald-700 text-white shadow-sm'
              : 'bg-white text-slate-700 ring-1 ring-slate-200 hover:bg-emerald-50 hover:text-emerald-900'
          "
          :aria-current="activeTab.query === tab.query ? 'page' : undefined"
        >
          {{ tab.label }}
        </RouterLink>
      </nav>

      <div
        v-if="isLoading"
        class="mt-8 rounded-3xl border border-sky-100 bg-sky-50 px-6 py-10 text-center font-bold text-sky-950"
        role="status"
      >
        {{ activeTab.loadingMessage }}
      </div>

      <div
        v-else-if="hasError"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
        role="alert"
      >
        <p class="text-lg font-black text-red-950">No pudimos cargar las publicaciones.</p>
        <p class="mt-2 text-sm leading-6 text-red-800">Intenta nuevamente en unos momentos.</p>
      </div>

      <div
        v-else-if="isEmpty"
        class="mt-8 rounded-3xl border border-emerald-100 bg-emerald-50 px-6 py-10 text-center text-lg font-bold text-emerald-950"
      >
        {{ activeTab.emptyMessage }}
      </div>

      <template v-else>
        <div class="mt-8 grid gap-5">
          <article
            v-for="post in items"
            :key="post.id"
            class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg"
          >
            <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
              <div>
                <div class="flex flex-wrap items-center gap-2 text-xs font-black uppercase">
                  <span class="rounded-full bg-emerald-100 px-3 py-1 text-emerald-900">
                    {{ adminPostStatusLabel(post.status) }}
                  </span>
                  <span class="rounded-full bg-sky-100 px-3 py-1 text-sky-950">
                    {{ post.section.name }}
                  </span>
                  <span
                    v-if="post.hasSourceQuestion"
                    class="rounded-full bg-cyan-100 px-3 py-1 text-cyan-950"
                  >
                    Nació de una pregunta
                  </span>
                </div>

                <h2 class="mt-4 text-2xl font-black text-slate-950">
                  {{ displayTitle(post.title) }}
                </h2>
                <p class="mt-3 text-sm font-bold text-slate-500">
                  {{ relevantDateLabel(post.status) }} · {{ formatDate(relevantDate(post)) }}
                </p>
              </div>

              <RouterLink
                :to="{ name: 'admin-post-detail', params: { id: post.id } }"
                class="inline-flex shrink-0 rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950"
              >
                Abrir
              </RouterLink>
            </div>
          </article>
        </div>

        <nav
          v-if="hasMultiplePages"
          class="mt-8 flex flex-col items-center justify-between gap-4 rounded-3xl bg-white p-4 shadow-sm ring-1 ring-slate-200 sm:flex-row"
          :aria-label="`Paginación de publicaciones ${activeTab.label.toLowerCase()}`"
        >
          <button
            type="button"
            class="rounded-2xl bg-slate-100 px-5 py-3 text-sm font-black text-slate-800 transition hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
            :disabled="currentPage === 0 || isLoading"
            @click="previousPage"
          >
            Anterior
          </button>

          <p class="text-sm font-black text-slate-700">
            Página {{ currentPage + 1 }} de {{ totalPages }}
          </p>

          <button
            type="button"
            class="rounded-2xl bg-slate-100 px-5 py-3 text-sm font-black text-slate-800 transition hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
            :disabled="currentPage + 1 >= totalPages || isLoading"
            @click="nextPage"
          >
            Siguiente
          </button>
        </nav>
      </template>
    </section>

    <div
      v-if="isCreateOpen"
      class="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-5 py-8"
      role="dialog"
      aria-modal="true"
      aria-labelledby="create-post-title"
      aria-describedby="create-post-description"
      @keydown.esc.prevent="closeCreateModal"
    >
      <section class="w-full max-w-lg rounded-3xl bg-white p-6 shadow-2xl">
        <h2 id="create-post-title" class="text-2xl font-black text-slate-950">
          Nueva publicación
        </h2>
        <p id="create-post-description" class="mt-3 leading-7 text-slate-700">
          Elige la sección inicial. El borrador se abrirá en el editor para completar título,
          contenido y hashtags antes de publicarlo.
        </p>

        <form class="mt-6 grid gap-5" @submit.prevent="createPost">
          <div>
            <label for="create-post-section" class="text-sm font-black uppercase text-emerald-700">
              Sección
            </label>
            <select
              id="create-post-section"
              v-model="createSectionSlug"
              class="mt-3 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100 disabled:cursor-not-allowed disabled:bg-slate-100"
              :disabled="isLoadingSections || isCreating || !sections.length"
              :aria-invalid="createError ? 'true' : undefined"
              :aria-describedby="createError ? 'create-post-error' : undefined"
            >
              <option value="" disabled>Selecciona una sección</option>
              <optgroup v-if="workshopSections.length" label="Talleres">
                <option
                  v-for="section in workshopSections"
                  :key="section.slug"
                  :value="section.slug"
                >
                  {{ section.name }}
                </option>
              </optgroup>
              <optgroup v-if="examSections.length" label="Parciales">
                <option
                  v-for="section in examSections"
                  :key="section.slug"
                  :value="section.slug"
                >
                  {{ section.name }}
                </option>
              </optgroup>
            </select>
          </div>

          <p
            v-if="isLoadingSections"
            class="rounded-2xl bg-sky-50 px-4 py-3 text-sm font-bold text-sky-950"
            role="status"
          >
            Cargando secciones...
          </p>
          <p
            v-if="createError"
            id="create-post-error"
            class="rounded-2xl bg-red-50 px-4 py-3 text-sm font-bold text-red-900"
            role="alert"
          >
            {{ createError }}
          </p>

          <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <button
              ref="createCancelButton"
              type="button"
              class="rounded-2xl bg-slate-100 px-5 py-3 text-sm font-black text-slate-800 transition hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
              :disabled="isCreating"
              @click="closeCreateModal"
            >
              Cancelar
            </button>
            <button
              type="submit"
              class="rounded-2xl bg-emerald-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800 disabled:cursor-not-allowed disabled:bg-slate-400"
              :disabled="isCreating || isLoadingSections || !createSectionSlug"
            >
              {{ isCreating ? 'Creando...' : 'Crear borrador' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </main>
</template>
