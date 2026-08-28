<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

interface TurnstileRenderOptions {
  sitekey: string
  action: string
  callback: (token: string) => void
  'expired-callback': () => void
  'error-callback': () => void
}

interface TurnstileApi {
  render: (container: HTMLElement, options: TurnstileRenderOptions) => string
  reset: (widgetId?: string) => void
  remove: (widgetId: string) => void
}

declare global {
  interface Window {
    turnstile?: TurnstileApi
  }
}

const SCRIPT_ID = 'cloudflare-turnstile-script'
const SCRIPT_SRC = 'https://challenges.cloudflare.com/turnstile/v0/api.js'

const props = defineProps<{
  siteKey: string
  action: string
}>()

const emit = defineEmits<{
  verified: [token: string]
  expired: []
  error: []
}>()

const container = ref<HTMLElement | null>(null)
const widgetId = ref<string | null>(null)
const isLoading = ref(true)
const hasError = ref(false)

onMounted(renderWidget)
onUnmounted(removeWidget)

defineExpose({ reset })

async function renderWidget() {
  if (!props.siteKey || !container.value) {
    isLoading.value = false
    return
  }

  try {
    await loadTurnstileScript()

    if (!window.turnstile || !container.value) {
      throw new Error('Turnstile is unavailable')
    }

    widgetId.value = window.turnstile.render(container.value, {
      sitekey: props.siteKey,
      action: props.action,
      callback: (token: string) => emit('verified', token),
      'expired-callback': () => emit('expired'),
      'error-callback': () => emit('error'),
    })
    hasError.value = false
  } catch {
    hasError.value = true
    emit('error')
  } finally {
    isLoading.value = false
  }
}

function reset() {
  if (window.turnstile && widgetId.value) {
    window.turnstile.reset(widgetId.value)
  }
}

function removeWidget() {
  if (window.turnstile && widgetId.value) {
    window.turnstile.remove(widgetId.value)
  }
}

function loadTurnstileScript() {
  if (window.turnstile) {
    return Promise.resolve()
  }

  const existingScript = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null
  if (existingScript) {
    return waitForScript(existingScript)
  }

  const script = document.createElement('script')
  script.id = SCRIPT_ID
  script.src = SCRIPT_SRC
  script.async = true
  script.defer = true
  document.head.appendChild(script)

  return waitForScript(script)
}

function waitForScript(script: HTMLScriptElement) {
  return new Promise<void>((resolve, reject) => {
    script.addEventListener('load', () => resolve(), { once: true })
    script.addEventListener('error', () => reject(new Error('Turnstile script failed')), {
      once: true,
    })
  })
}
</script>

<template>
  <div class="rounded-2xl border border-cyan-100 bg-cyan-50/60 p-4">
    <p id="turnstile-help" class="text-sm font-black text-slate-950">Verificación anti-abuso</p>
    <p class="mt-2 text-sm leading-6 text-slate-600">
      Completa esta verificación para confirmar que el envío es legítimo.
    </p>
    <div
      ref="container"
      class="mt-4 min-h-[65px]"
      aria-describedby="turnstile-help"
    />
    <p v-if="isLoading" class="mt-2 text-sm font-bold text-slate-600">Cargando verificación...</p>
    <p v-if="hasError" class="mt-2 text-sm font-bold text-red-800" role="alert">
      No pudimos cargar la verificación. Intenta recargar la página.
    </p>
  </div>
</template>
