<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import {
  createAdminTag,
  deleteAdminTag,
  getAdminTags,
  renameAdminTag,
} from '@/services/api/adminTagService'
import { signOut } from '@/services/firebase/authService'
import type { AdminTag } from '@/types/adminTag'

const router = useRouter()
const tags = ref<AdminTag[]>([])
const isLoading = ref(true)
const hasError = ref(false)
const createName = ref('')
const isCreating = ref(false)
const createError = ref('')
const createSuccess = ref('')
const renameState = reactive({
  tag: null as AdminTag | null,
  name: '',
  isSaving: false,
  error: '',
})
const deleteState = reactive({
  tag: null as AdminTag | null,
  isDeleting: false,
  error: '',
})
const renameCancelButton = ref<HTMLButtonElement | null>(null)
const deleteCancelButton = ref<HTMLButtonElement | null>(null)
let lastFocusedElement: HTMLElement | null = null

const isEmpty = computed(() => !isLoading.value && !hasError.value && tags.value.length === 0)

void loadTags()

async function loadTags() {
  isLoading.value = true
  hasError.value = false

  try {
    tags.value = await getAdminTags()
  } catch (error) {
    await handleError(error, () => {
      hasError.value = true
      tags.value = []
    })
  } finally {
    isLoading.value = false
  }
}

async function createTag() {
  if (isCreating.value || !createName.value.trim()) {
    return
  }

  isCreating.value = true
  createError.value = ''
  createSuccess.value = ''

  try {
    await createAdminTag({ name: createName.value })
    createName.value = ''
    createSuccess.value = 'Hashtag creado.'
    await loadTags()
  } catch (error) {
    await handleError(error, () => {
      createError.value = 'No pudimos crear el hashtag. Revisa que el nombre no exista ya.'
    })
  } finally {
    isCreating.value = false
  }
}

async function openRename(tag: AdminTag) {
  renameState.tag = tag
  renameState.name = tag.name
  renameState.error = ''
  lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null
  await nextTick()
  renameCancelButton.value?.focus()
}

function closeRename() {
  if (renameState.isSaving) {
    return
  }

  renameState.tag = null
  lastFocusedElement?.focus()
}

async function saveRename() {
  if (!renameState.tag || renameState.isSaving || !renameState.name.trim()) {
    return
  }

  renameState.isSaving = true
  renameState.error = ''

  try {
    await renameAdminTag(renameState.tag.id, { name: renameState.name })
    renameState.tag = null
    await loadTags()
  } catch (error) {
    await handleError(error, () => {
      renameState.error = 'No pudimos renombrar el hashtag. Revisa que el nombre no exista ya.'
    })
  } finally {
    renameState.isSaving = false
  }
}

async function openDelete(tag: AdminTag) {
  if (tag.usageCount > 0) {
    return
  }

  deleteState.tag = tag
  deleteState.error = ''
  lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null
  await nextTick()
  deleteCancelButton.value?.focus()
}

function closeDelete() {
  if (deleteState.isDeleting) {
    return
  }

  deleteState.tag = null
  lastFocusedElement?.focus()
}

async function confirmDelete() {
  if (!deleteState.tag || deleteState.isDeleting) {
    return
  }

  deleteState.isDeleting = true
  deleteState.error = ''

  try {
    await deleteAdminTag(deleteState.tag.id)
    deleteState.tag = null
    await loadTags()
  } catch (error) {
    await handleError(error, () => {
      deleteState.error = 'No pudimos eliminar el hashtag.'
    })
  } finally {
    deleteState.isDeleting = false
  }
}

async function handleError(error: unknown, fallback: () => void) {
  if (isAdminAuthorizationError(error)) {
    await signOut().catch(() => undefined)
    await router.push({ name: 'admin-login', query: { reason: 'forbidden' } })
    return
  }

  fallback()
}

function usageLabel(count: number): string {
  if (count === 0) {
    return 'Sin uso'
  }

  return `Usado en ${count} ${count === 1 ? 'publicación' : 'publicaciones'}`
}
</script>

<template>
  <main class="px-5 py-10 sm:px-6">
    <section class="mx-auto max-w-6xl">
      <header class="max-w-3xl">
        <p class="text-sm font-black uppercase text-emerald-700">Hashtags</p>
        <h1 class="mt-3 text-4xl font-black">Hashtags administrados</h1>
        <p class="mt-4 text-lg leading-8 text-slate-700">
          Crea y organiza etiquetas reutilizables para clasificar publicaciones.
        </p>
      </header>

      <form
        class="mt-8 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"
        aria-labelledby="new-tag-title"
        @submit.prevent="createTag"
      >
        <h2 id="new-tag-title" class="text-2xl font-black text-slate-950">Nuevo hashtag</h2>
        <div class="mt-5 grid gap-4 md:grid-cols-[1fr_auto] md:items-end">
          <div>
            <label for="new-tag-name" class="text-sm font-black uppercase text-emerald-700">
              Nombre
            </label>
            <input
              id="new-tag-name"
              v-model="createName"
              type="text"
              maxlength="80"
              placeholder="Escribe el nombre del hashtag."
              class="mt-3 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
            />
          </div>
          <button
            type="submit"
            class="rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950 disabled:cursor-not-allowed disabled:bg-slate-400"
            :disabled="isCreating || !createName.trim()"
          >
            {{ isCreating ? 'Creando...' : 'Crear hashtag' }}
          </button>
        </div>
        <p v-if="createError" class="mt-4 rounded-2xl bg-red-50 px-4 py-3 text-sm font-bold text-red-900" role="alert">
          {{ createError }}
        </p>
        <p
          v-else-if="createSuccess"
          class="mt-4 rounded-2xl bg-emerald-50 px-4 py-3 text-sm font-bold text-emerald-900"
          role="status"
        >
          {{ createSuccess }}
        </p>
      </form>

      <div
        v-if="isLoading"
        class="mt-8 rounded-3xl border border-sky-100 bg-sky-50 px-6 py-10 text-center font-bold text-sky-950"
        role="status"
      >
        Cargando hashtags...
      </div>

      <div
        v-else-if="hasError"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
        role="alert"
      >
        <p class="text-lg font-black text-red-950">No pudimos cargar los hashtags.</p>
        <p class="mt-2 text-sm leading-6 text-red-800">Intenta nuevamente en unos momentos.</p>
      </div>

      <div
        v-else-if="isEmpty"
        class="mt-8 rounded-3xl border border-emerald-100 bg-emerald-50 px-6 py-10 text-center text-lg font-bold text-emerald-950"
      >
        Aún no hay hashtags creados.
      </div>

      <div v-else class="mt-8 grid gap-5">
        <article
          v-for="tag in tags"
          :key="tag.id"
          class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg"
        >
          <div class="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
            <div>
              <h2 class="text-2xl font-black text-slate-950">#{{ tag.name }}</h2>
              <p class="mt-2 text-sm font-bold text-slate-500">{{ tag.slug }}</p>
              <p class="mt-3 text-sm font-black text-emerald-800">{{ usageLabel(tag.usageCount) }}</p>
              <p v-if="tag.usageCount > 0" class="mt-2 text-sm leading-6 text-slate-600">
                Quita este hashtag de las publicaciones antes de eliminarlo.
              </p>
            </div>

            <div class="flex flex-wrap gap-3">
              <RouterLink
                :to="{ name: 'hashtag-detail', params: { slug: tag.slug } }"
                class="rounded-2xl bg-emerald-50 px-5 py-3 text-sm font-black text-emerald-900 transition hover:bg-emerald-100 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
              >
                Ver hashtag
              </RouterLink>
              <button
                type="button"
                class="rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950"
                @click="openRename(tag)"
              >
                Renombrar
              </button>
              <button
                type="button"
                class="rounded-2xl bg-red-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-red-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                :disabled="tag.usageCount > 0"
                @click="openDelete(tag)"
              >
                Eliminar
              </button>
            </div>
          </div>
        </article>
      </div>
    </section>

    <div
      v-if="renameState.tag"
      class="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-5 py-8"
      role="dialog"
      aria-modal="true"
      aria-labelledby="rename-tag-title"
      @keydown.esc.prevent="closeRename"
    >
      <section class="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
        <h2 id="rename-tag-title" class="text-2xl font-black text-slate-950">Renombrar hashtag</h2>
        <p class="mt-3 text-sm font-bold text-slate-600">
          Nombre actual: #{{ renameState.tag.name }}
        </p>
        <p class="mt-2 text-sm leading-6 text-slate-600">La URL del hashtag se conservará.</p>

        <label for="rename-tag-name" class="mt-5 block text-sm font-black uppercase text-emerald-700">
          Nuevo nombre
        </label>
        <input
          id="rename-tag-name"
          v-model="renameState.name"
          type="text"
          maxlength="80"
          class="mt-3 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
        />
        <p v-if="renameState.error" class="mt-4 rounded-2xl bg-red-50 px-4 py-3 text-sm font-bold text-red-900" role="alert">
          {{ renameState.error }}
        </p>

        <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            ref="renameCancelButton"
            type="button"
            class="rounded-2xl bg-slate-100 px-5 py-3 text-sm font-black text-slate-800 transition hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
            :disabled="renameState.isSaving"
            @click="closeRename"
          >
            Cancelar
          </button>
          <button
            type="button"
            class="rounded-2xl bg-emerald-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            :disabled="renameState.isSaving || !renameState.name.trim()"
            @click="saveRename"
          >
            {{ renameState.isSaving ? 'Guardando...' : 'Guardar nombre' }}
          </button>
        </div>
      </section>
    </div>

    <div
      v-if="deleteState.tag"
      class="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-5 py-8"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-tag-title"
      aria-describedby="delete-tag-description"
      @keydown.esc.prevent="closeDelete"
    >
      <section class="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
        <h2 id="delete-tag-title" class="text-2xl font-black text-slate-950">
          ¿Eliminar este hashtag?
        </h2>
        <p id="delete-tag-description" class="mt-3 leading-7 text-slate-700">
          Esta acción eliminará el hashtag. No afecta publicaciones porque actualmente no está en uso.
        </p>
        <p v-if="deleteState.error" class="mt-4 rounded-2xl bg-red-50 px-4 py-3 text-sm font-bold text-red-900" role="alert">
          {{ deleteState.error }}
        </p>

        <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            ref="deleteCancelButton"
            type="button"
            class="rounded-2xl bg-slate-100 px-5 py-3 text-sm font-black text-slate-800 transition hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
            :disabled="deleteState.isDeleting"
            @click="closeDelete"
          >
            Cancelar
          </button>
          <button
            type="button"
            class="rounded-2xl bg-red-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-red-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-800 disabled:cursor-not-allowed disabled:bg-slate-400"
            :disabled="deleteState.isDeleting"
            @click="confirmDelete"
          >
            {{ deleteState.isDeleting ? 'Eliminando...' : 'Eliminar hashtag' }}
          </button>
        </div>
      </section>
    </div>
  </main>
</template>
