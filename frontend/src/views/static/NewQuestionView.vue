<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'

import PageBanner from '@/components/PageBanner.vue'
import { createQuestion } from '@/services/api/questionService'
import { getSections } from '@/services/api/sectionService'
import type { Section } from '@/types/section'
import {
  NICKNAME_MAX_LENGTH,
  QUESTION_IMAGE_ACCEPTED_TYPES,
  QUESTION_IMAGE_MAX_SIZE_BYTES,
  QUESTION_MAX_LENGTH,
} from '@/types/studentQuestion'

type SubmitState = 'INITIAL' | 'SUBMITTING' | 'SUCCESS' | 'ERROR'

const sections = ref<Section[]>([])
const isLoadingSections = ref(true)
const hasSectionsError = ref(false)
const submitState = ref<SubmitState>('INITIAL')
const selectedImage = ref<File | null>(null)
const imagePreviewUrl = ref<string | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)

const form = reactive({
  nickname: '',
  sectionSlug: '',
  question: '',
})

const errors = reactive({
  sectionSlug: '',
  question: '',
  nickname: '',
  image: '',
})

const talleres = computed(() => sections.value.filter((section) => section.type === 'TALLER'))
const parciales = computed(() => sections.value.filter((section) => section.type === 'PARCIAL'))
const questionLength = computed(() => form.question.length)
const isSubmitting = computed(() => submitState.value === 'SUBMITTING')
const canSubmit = computed(() => !isSubmitting.value && !isLoadingSections.value)
const selectedImageSize = computed(() =>
  selectedImage.value ? formatFileSize(selectedImage.value.size) : '',
)

onMounted(loadSections)
onUnmounted(revokePreviewUrl)

async function loadSections() {
  isLoadingSections.value = true
  hasSectionsError.value = false

  try {
    sections.value = await getSections()
  } catch {
    hasSectionsError.value = true
    sections.value = []
  } finally {
    isLoadingSections.value = false
  }
}

async function submitQuestion() {
  if (isSubmitting.value) {
    return
  }

  submitState.value = 'INITIAL'

  if (!validateForm()) {
    return
  }

  submitState.value = 'SUBMITTING'

  try {
    await createQuestion({
      data: {
        sectionSlug: form.sectionSlug,
        nickname: normalizeOptionalText(form.nickname),
        question: form.question.trim(),
      },
      image: selectedImage.value,
    })
    form.nickname = ''
    form.sectionSlug = ''
    form.question = ''
    clearSelectedImage()
    submitState.value = 'SUCCESS'
  } catch {
    submitState.value = 'ERROR'
  }
}

function validateForm() {
  errors.sectionSlug = ''
  errors.question = ''
  errors.nickname = ''

  if (!form.sectionSlug) {
    errors.sectionSlug = 'Selecciona una sección.'
  }

  if (!form.question.trim()) {
    errors.question = 'Escribe tu pregunta.'
  } else if (form.question.length > QUESTION_MAX_LENGTH) {
    errors.question = `La pregunta debe tener ${QUESTION_MAX_LENGTH} caracteres o menos.`
  }

  if (form.nickname.length > NICKNAME_MAX_LENGTH) {
    errors.nickname = `El apodo debe tener ${NICKNAME_MAX_LENGTH} caracteres o menos.`
  }

  if (selectedImage.value) {
    errors.image = validateImage(selectedImage.value)
  }

  return !errors.sectionSlug && !errors.question && !errors.nickname && !errors.image
}

function normalizeOptionalText(value: string) {
  const trimmedValue = value.trim()

  return trimmedValue ? trimmedValue : null
}

function handleImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null

  errors.image = ''

  if (!file) {
    clearSelectedImage()
    return
  }

  const validationMessage = validateImage(file)
  if (validationMessage) {
    errors.image = validationMessage
    clearSelectedImage()
    return
  }

  revokePreviewUrl()
  selectedImage.value = file
  imagePreviewUrl.value = URL.createObjectURL(file)
}

function removeSelectedImage() {
  clearSelectedImage()
}

function clearSelectedImage() {
  revokePreviewUrl()
  selectedImage.value = null

  if (imageInput.value) {
    imageInput.value.value = ''
  }
}

function revokePreviewUrl() {
  if (imagePreviewUrl.value) {
    URL.revokeObjectURL(imagePreviewUrl.value)
    imagePreviewUrl.value = null
  }
}

function validateImage(file: File) {
  if (!QUESTION_IMAGE_ACCEPTED_TYPES.some((acceptedType) => acceptedType === file.type)) {
    return 'Adjunta una imagen JPEG o PNG.'
  }

  if (file.size > QUESTION_IMAGE_MAX_SIZE_BYTES) {
    return 'La imagen debe pesar 5 MB o menos.'
  }

  return ''
}

function formatFileSize(size: number) {
  if (size < 1024 * 1024) {
    return `${Math.max(1, Math.round(size / 1024))} KB`
  }

  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<template>
  <div>
    <PageBanner
      eyebrow="Participación"
      title="Agregar una pregunta"
      description="Envía una duda para que el profesor pueda revisarla y convertirla posteriormente en contenido útil para el curso."
    />

    <section class="mx-auto max-w-4xl px-5 py-12 sm:px-6 sm:py-16">
      <form
        class="rounded-3xl border border-cyan-100 bg-white p-6 shadow-sm sm:p-8"
        novalidate
        @submit.prevent="submitQuestion"
      >
        <div class="max-w-2xl">
          <p class="text-sm font-black uppercase text-emerald-800">Pregunta pública para revisión</p>
          <h1 class="mt-3 text-3xl font-black text-slate-950">Cuéntanos tu duda</h1>
          <p class="mt-4 leading-7 text-slate-700">
            Tu pregunta quedará pendiente de revisión. No se publicará automáticamente.
          </p>
        </div>

        <div
          v-if="submitState === 'SUCCESS'"
          class="mt-8 rounded-2xl border border-emerald-200 bg-emerald-50 p-5 text-emerald-950"
          role="status"
        >
          <p class="font-black">Tu pregunta fue enviada y quedó pendiente de revisión.</p>
          <p class="mt-2 text-sm leading-6">
            Será revisada por el profesor antes de aparecer como contenido público.
          </p>
        </div>

        <div
          v-else-if="submitState === 'ERROR'"
          class="mt-8 rounded-2xl border border-red-200 bg-red-50 p-5 text-red-950"
          role="alert"
        >
          <p class="font-black">No pudimos enviar tu pregunta.</p>
          <p class="mt-2 text-sm leading-6">Revisa los campos e intenta nuevamente en unos momentos.</p>
        </div>

        <div class="mt-8 grid gap-7">
          <div>
            <label for="nickname" class="block text-sm font-black text-slate-950">
              Nombre o apodo
            </label>
            <p id="nickname-help" class="mt-2 text-sm leading-6 text-slate-600">
              Opcional. Puedes dejarlo vacío para enviar la pregunta de forma anónima.
            </p>
            <input
              id="nickname"
              v-model="form.nickname"
              type="text"
              :maxlength="NICKNAME_MAX_LENGTH"
              aria-describedby="nickname-help nickname-error"
              class="mt-3 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-slate-950 outline-none transition focus:border-sky-800 focus:bg-white focus:ring-4 focus:ring-sky-100 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="isSubmitting"
            />
            <p v-if="errors.nickname" id="nickname-error" class="mt-2 text-sm font-bold text-red-800">
              {{ errors.nickname }}
            </p>
          </div>

          <div>
            <label for="section" class="block text-sm font-black text-slate-950">Sección</label>
            <p id="section-help" class="mt-2 text-sm leading-6 text-slate-600">
              Selecciona el taller o parcial relacionado con tu pregunta.
            </p>

            <p
              v-if="isLoadingSections"
              class="mt-3 rounded-2xl border border-sky-200 bg-sky-50 p-4 text-sky-950"
            >
              Cargando secciones...
            </p>

            <p
              v-else-if="hasSectionsError"
              class="mt-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-red-950"
              role="alert"
            >
              No pudimos cargar las secciones. Intenta recargar la página.
            </p>

            <select
              v-else
              id="section"
              v-model="form.sectionSlug"
              aria-describedby="section-help section-error"
              class="mt-3 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-slate-950 outline-none transition focus:border-sky-800 focus:bg-white focus:ring-4 focus:ring-sky-100 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="isSubmitting"
            >
              <option value="">Selecciona una sección</option>
              <optgroup v-if="talleres.length > 0" label="Talleres">
                <option v-for="section in talleres" :key="section.id" :value="section.slug">
                  {{ section.name }}
                </option>
              </optgroup>
              <optgroup v-if="parciales.length > 0" label="Parciales">
                <option v-for="section in parciales" :key="section.id" :value="section.slug">
                  {{ section.name }}
                </option>
              </optgroup>
            </select>
            <p v-if="errors.sectionSlug" id="section-error" class="mt-2 text-sm font-bold text-red-800">
              {{ errors.sectionSlug }}
            </p>
          </div>

          <div>
            <div class="flex flex-wrap items-end justify-between gap-3">
              <label for="question" class="block text-sm font-black text-slate-950">Pregunta</label>
              <p class="text-sm font-bold text-slate-600">
                {{ questionLength }} / {{ QUESTION_MAX_LENGTH }}
              </p>
            </div>
            <textarea
              id="question"
              v-model="form.question"
              rows="8"
              aria-describedby="question-error"
              class="mt-3 w-full resize-y rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 text-slate-950 outline-none transition focus:border-sky-800 focus:bg-white focus:ring-4 focus:ring-sky-100 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="isSubmitting"
            />
            <p v-if="errors.question" id="question-error" class="mt-2 text-sm font-bold text-red-800">
              {{ errors.question }}
            </p>
          </div>

          <div>
            <label for="image" class="block text-sm font-black text-slate-950">
              Adjuntar imagen (opcional)
            </label>
            <p id="image-help" class="mt-2 text-sm leading-6 text-slate-600">
              Puedes adjuntar una captura, gráfica o procedimiento en JPEG o PNG. Máximo 5 MB.
            </p>
            <input
              id="image"
              ref="imageInput"
              type="file"
              accept="image/jpeg,image/png"
              aria-describedby="image-help image-error"
              class="mt-3 w-full rounded-2xl border border-dashed border-cyan-200 bg-cyan-50/60 px-4 py-4 text-sm font-bold text-slate-800 outline-none transition file:mr-4 file:rounded-full file:border-0 file:bg-sky-950 file:px-4 file:py-2 file:text-sm file:font-black file:text-white hover:border-cyan-400 focus:border-sky-800 focus:bg-white focus:ring-4 focus:ring-sky-100 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="isSubmitting"
              @change="handleImageChange"
            />
            <p v-if="errors.image" id="image-error" class="mt-2 text-sm font-bold text-red-800">
              {{ errors.image }}
            </p>

            <div
              v-if="selectedImage"
              class="mt-4 overflow-hidden rounded-3xl border border-cyan-100 bg-white shadow-sm"
            >
              <img
                v-if="imagePreviewUrl"
                :src="imagePreviewUrl"
                :alt="`Vista previa de ${selectedImage.name}`"
                class="h-56 w-full object-contain bg-slate-100"
              />
              <div class="flex flex-wrap items-center justify-between gap-3 p-4">
                <div>
                  <p class="font-black text-slate-950">{{ selectedImage.name }}</p>
                  <p class="mt-1 text-sm font-bold text-slate-600">{{ selectedImageSize }}</p>
                </div>
                <button
                  type="button"
                  class="rounded-full border border-slate-300 px-4 py-2 text-sm font-black text-slate-800 transition hover:border-red-300 hover:bg-red-50 hover:text-red-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-red-700 disabled:cursor-not-allowed disabled:opacity-60"
                  :disabled="isSubmitting"
                  @click="removeSelectedImage"
                >
                  Quitar imagen
                </button>
              </div>
            </div>
          </div>
        </div>

        <button
          type="submit"
          class="mt-8 inline-flex w-full items-center justify-center rounded-full bg-sky-950 px-6 py-4 text-sm font-black uppercase text-white shadow-sm transition hover:bg-emerald-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-sky-800 disabled:cursor-not-allowed disabled:bg-slate-400 sm:w-auto"
          :disabled="!canSubmit"
        >
          {{ isSubmitting ? 'Enviando pregunta...' : 'Enviar pregunta' }}
        </button>
      </form>
    </section>
  </div>
</template>
