<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { isAdminAuthorizationError } from '@/services/api/adminErrors'
import {
  createAdminLink,
  deleteAdminLink,
  getAdminLinks,
  updateAdminLink,
} from '@/services/api/adminLinkService'
import { signOut } from '@/services/firebase/authService'
import type { AdminInterestingLink, UpsertAdminInterestingLinkRequest } from '@/types/interestingLink'

const router = useRouter()
const links = ref<AdminInterestingLink[]>([])
const isLoading = ref(true)
const hasError = ref(false)
const createForm = reactive({
  title: '',
  description: '',
  url: '',
  displayOrder: 0,
  active: true,
})
const isCreating = ref(false)
const createError = ref('')
const createSuccess = ref('')
const editState = reactive({
  link: null as AdminInterestingLink | null,
  title: '',
  description: '',
  url: '',
  displayOrder: 0,
  active: true,
  isSaving: false,
  error: '',
})
const deleteState = reactive({
  link: null as AdminInterestingLink | null,
  isDeleting: false,
  error: '',
})
const editTitleInput = ref<HTMLInputElement | HTMLInputElement[] | null>(null)
const deleteCancelButton = ref<HTMLButtonElement | null>(null)
let lastFocusedElement: HTMLElement | null = null

const isEmpty = computed(() => !isLoading.value && !hasError.value && links.value.length === 0)

void loadLinks()

async function loadLinks() {
  isLoading.value = true
  hasError.value = false

  try {
    links.value = await getAdminLinks()
  } catch (error) {
    await handleError(error, () => {
      hasError.value = true
      links.value = []
    })
  } finally {
    isLoading.value = false
  }
}

async function createLink() {
  if (isCreating.value) {
    return
  }

  const payload = toPayload(createForm)
  if (!payload) {
    createError.value = 'Escribe un título y una URL http o https válida.'
    createSuccess.value = ''
    return
  }

  isCreating.value = true
  createError.value = ''
  createSuccess.value = ''

  try {
    await createAdminLink(payload)
    resetCreateForm()
    createSuccess.value = 'Enlace creado.'
    await loadLinks()
  } catch (error) {
    await handleError(error, () => {
      createError.value = 'No pudimos crear el enlace. Revisa los datos e intenta nuevamente.'
    })
  } finally {
    isCreating.value = false
  }
}

async function startEdit(link: AdminInterestingLink) {
  editState.link = link
  editState.title = link.title
  editState.description = link.description ?? ''
  editState.url = link.url
  editState.displayOrder = link.displayOrder
  editState.active = link.active
  editState.error = ''
  lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null
  await nextTick()
  focusFirstElement(editTitleInput.value)
}

function cancelEdit() {
  if (editState.isSaving) {
    return
  }

  editState.link = null
  editState.error = ''
  lastFocusedElement?.focus()
}

async function saveEdit() {
  if (!editState.link || editState.isSaving) {
    return
  }

  const payload = toPayload(editState)
  if (!payload) {
    editState.error = 'Escribe un título y una URL http o https válida.'
    return
  }

  editState.isSaving = true
  editState.error = ''

  try {
    await updateAdminLink(editState.link.id, payload)
    editState.link = null
    await loadLinks()
  } catch (error) {
    await handleError(error, () => {
      editState.error = 'No pudimos guardar el enlace. Revisa los datos e intenta nuevamente.'
    })
  } finally {
    editState.isSaving = false
  }
}

async function openDelete(link: AdminInterestingLink) {
  deleteState.link = link
  deleteState.error = ''
  lastFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null
  await nextTick()
  deleteCancelButton.value?.focus()
}

function closeDelete() {
  if (deleteState.isDeleting) {
    return
  }

  deleteState.link = null
  deleteState.error = ''
  lastFocusedElement?.focus()
}

async function confirmDelete() {
  if (!deleteState.link || deleteState.isDeleting) {
    return
  }

  deleteState.isDeleting = true
  deleteState.error = ''

  try {
    await deleteAdminLink(deleteState.link.id)
    deleteState.link = null
    await loadLinks()
  } catch (error) {
    await handleError(error, () => {
      deleteState.error = 'No pudimos eliminar el enlace.'
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

function toPayload(form: {
  title: string
  description: string
  url: string
  displayOrder: number
  active: boolean
}): UpsertAdminInterestingLinkRequest | null {
  const title = form.title.trim()
  const description = form.description.trim()
  const url = form.url.trim()
  const displayOrder = Number(form.displayOrder)

  if (!title || !isHttpUrl(url) || !Number.isInteger(displayOrder) || displayOrder < 0) {
    return null
  }

  return {
    title,
    description: description || null,
    url,
    displayOrder,
    active: form.active,
  }
}

function isHttpUrl(value: string): boolean {
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

function resetCreateForm() {
  createForm.title = ''
  createForm.description = ''
  createForm.url = ''
  createForm.displayOrder = 0
  createForm.active = true
}

function focusFirstElement(element: HTMLInputElement | HTMLInputElement[] | null) {
  const target = Array.isArray(element) ? element[0] : element
  target?.focus()
}

function statusLabel(link: AdminInterestingLink): string {
  return link.active ? 'Activo' : 'Inactivo'
}
</script>

<template>
  <main class="px-5 py-10 sm:px-6">
    <section class="mx-auto max-w-6xl">
      <header class="max-w-3xl">
        <p class="text-sm font-black uppercase text-emerald-700">Enlaces</p>
        <h1 class="mt-3 text-4xl font-black">Enlaces de interés</h1>
        <p class="mt-4 text-lg leading-8 text-slate-700">
          Administra los recursos externos que aparecerán en la página pública de enlaces.
        </p>
      </header>

      <form
        class="mt-8 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"
        aria-labelledby="new-link-title"
        @submit.prevent="createLink"
      >
        <h2 id="new-link-title" class="text-2xl font-black text-slate-950">Crear enlace</h2>

        <div class="mt-5 grid gap-4 lg:grid-cols-[1fr_1fr_auto] lg:items-end">
          <div>
            <label for="new-link-title-input" class="text-sm font-black uppercase text-emerald-700">
              Título
            </label>
            <input
              id="new-link-title-input"
              v-model="createForm.title"
              type="text"
              maxlength="160"
              class="mt-3 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
            />
          </div>

          <div>
            <label for="new-link-url" class="text-sm font-black uppercase text-emerald-700">
              URL
            </label>
            <input
              id="new-link-url"
              v-model="createForm.url"
              type="url"
              maxlength="2048"
              placeholder="https://..."
              class="mt-3 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
            />
          </div>

          <button
            type="submit"
            class="rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950 disabled:cursor-not-allowed disabled:bg-slate-400"
            :disabled="isCreating"
          >
            {{ isCreating ? 'Creando...' : 'Crear enlace' }}
          </button>
        </div>

        <div class="mt-4 grid gap-4 md:grid-cols-[1fr_auto_auto] md:items-end">
          <div>
            <label for="new-link-description" class="text-sm font-black uppercase text-emerald-700">
              Descripción
            </label>
            <textarea
              id="new-link-description"
              v-model="createForm.description"
              rows="3"
              class="mt-3 w-full resize-y rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
            />
          </div>

          <div>
            <label for="new-link-order" class="text-sm font-black uppercase text-emerald-700">
              Orden
            </label>
            <input
              id="new-link-order"
              v-model.number="createForm.displayOrder"
              type="number"
              min="0"
              step="1"
              class="mt-3 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100 md:w-28"
            />
          </div>

          <label class="inline-flex items-center gap-3 rounded-2xl bg-emerald-50 px-4 py-3 text-sm font-black text-emerald-900">
            <input v-model="createForm.active" type="checkbox" class="size-4 accent-emerald-700" />
            Activo
          </label>
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
        Cargando enlaces...
      </div>

      <div
        v-else-if="hasError"
        class="mt-8 rounded-3xl border border-red-100 bg-red-50 px-6 py-10 text-center"
        role="alert"
      >
        <p class="text-lg font-black text-red-950">No pudimos cargar los enlaces.</p>
        <p class="mt-2 text-sm leading-6 text-red-800">Intenta nuevamente en unos momentos.</p>
      </div>

      <div
        v-else-if="isEmpty"
        class="mt-8 rounded-3xl border border-emerald-100 bg-emerald-50 px-6 py-10 text-center text-lg font-bold text-emerald-950"
      >
        Aún no hay enlaces creados.
      </div>

      <div v-else class="mt-8 grid gap-5">
        <article
          v-for="link in links"
          :key="link.id"
          class="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-0.5 hover:shadow-lg"
        >
          <form v-if="editState.link?.id === link.id" class="grid gap-4" @submit.prevent="saveEdit">
            <div class="grid gap-4 md:grid-cols-2">
              <div>
                <label :for="`edit-link-title-${link.id}`" class="text-sm font-black uppercase text-emerald-700">
                  Título
                </label>
                <input
                  :id="`edit-link-title-${link.id}`"
                  ref="editTitleInput"
                  v-model="editState.title"
                  type="text"
                  maxlength="160"
                  class="mt-3 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
                />
              </div>

              <div>
                <label :for="`edit-link-url-${link.id}`" class="text-sm font-black uppercase text-emerald-700">
                  URL
                </label>
                <input
                  :id="`edit-link-url-${link.id}`"
                  v-model="editState.url"
                  type="url"
                  maxlength="2048"
                  class="mt-3 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
                />
              </div>
            </div>

            <div class="grid gap-4 md:grid-cols-[1fr_auto_auto] md:items-end">
              <div>
                <label :for="`edit-link-description-${link.id}`" class="text-sm font-black uppercase text-emerald-700">
                  Descripción
                </label>
                <textarea
                  :id="`edit-link-description-${link.id}`"
                  v-model="editState.description"
                  rows="3"
                  class="mt-3 w-full resize-y rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100"
                />
              </div>

              <div>
                <label :for="`edit-link-order-${link.id}`" class="text-sm font-black uppercase text-emerald-700">
                  Orden
                </label>
                <input
                  :id="`edit-link-order-${link.id}`"
                  v-model.number="editState.displayOrder"
                  type="number"
                  min="0"
                  step="1"
                  class="mt-3 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-base font-bold text-slate-950 outline-none transition focus:border-emerald-700 focus:ring-4 focus:ring-emerald-100 md:w-28"
                />
              </div>

              <label class="inline-flex items-center gap-3 rounded-2xl bg-emerald-50 px-4 py-3 text-sm font-black text-emerald-900">
                <input v-model="editState.active" type="checkbox" class="size-4 accent-emerald-700" />
                Activo
              </label>
            </div>

            <p v-if="editState.error" class="rounded-2xl bg-red-50 px-4 py-3 text-sm font-bold text-red-900" role="alert">
              {{ editState.error }}
            </p>

            <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button
                type="button"
                class="rounded-2xl bg-slate-100 px-5 py-3 text-sm font-black text-slate-800 transition hover:bg-slate-200 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-slate-700 disabled:cursor-not-allowed disabled:text-slate-400"
                :disabled="editState.isSaving"
                @click="cancelEdit"
              >
                Cancelar
              </button>
              <button
                type="submit"
                class="rounded-2xl bg-emerald-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                :disabled="editState.isSaving"
              >
                {{ editState.isSaving ? 'Guardando...' : 'Guardar enlace' }}
              </button>
            </div>
          </form>

          <div v-else class="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2 text-xs font-black uppercase">
                <span
                  class="rounded-full px-3 py-1"
                  :class="link.active ? 'bg-emerald-100 text-emerald-900' : 'bg-slate-100 text-slate-600'"
                >
                  {{ statusLabel(link) }}
                </span>
                <span class="rounded-full bg-sky-100 px-3 py-1 text-sky-950">
                  Orden {{ link.displayOrder }}
                </span>
              </div>

              <h2 class="mt-4 break-words text-2xl font-black text-slate-950">{{ link.title }}</h2>
              <p class="mt-2 break-all text-sm font-bold text-sky-800">{{ link.url }}</p>
              <p v-if="link.description" class="mt-3 leading-7 text-slate-700">{{ link.description }}</p>
              <p v-else class="mt-3 text-sm font-bold text-slate-500">Sin descripción.</p>
            </div>

            <div class="flex flex-wrap gap-3">
              <a
                :href="link.url"
                target="_blank"
                rel="noopener noreferrer"
                class="rounded-2xl bg-emerald-50 px-5 py-3 text-sm font-black text-emerald-900 transition hover:bg-emerald-100 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-emerald-800"
              >
                Visitar
              </a>
              <button
                type="button"
                class="rounded-2xl bg-sky-950 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-sky-900 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-950"
                @click="startEdit(link)"
              >
                Editar
              </button>
              <button
                type="button"
                class="rounded-2xl bg-red-700 px-5 py-3 text-sm font-black text-white shadow-sm transition hover:bg-red-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-800"
                @click="openDelete(link)"
              >
                Eliminar
              </button>
            </div>
          </div>
        </article>
      </div>
    </section>

    <div
      v-if="deleteState.link"
      class="fixed inset-0 z-50 grid place-items-center bg-slate-950/60 px-5 py-8"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-link-title"
      aria-describedby="delete-link-description"
      @keydown.esc.prevent="closeDelete"
    >
      <section class="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl">
        <h2 id="delete-link-title" class="text-2xl font-black text-slate-950">¿Eliminar este enlace?</h2>
        <p id="delete-link-description" class="mt-3 leading-7 text-slate-700">
          Se eliminará únicamente este enlace de interés. No afecta publicaciones ni preguntas.
        </p>
        <p class="mt-3 rounded-2xl bg-slate-50 px-4 py-3 text-sm font-black text-slate-700">
          {{ deleteState.link.title }}
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
            {{ deleteState.isDeleting ? 'Eliminando...' : 'Eliminar enlace' }}
          </button>
        </div>
      </section>
    </div>
  </main>
</template>
