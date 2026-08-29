import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { createQuestion } from '@/services/api/questionService'
import { getSections } from '@/services/api/sectionService'
import type { Section } from '@/types/section'
import { QUESTION_MAX_LENGTH } from '@/types/studentQuestion'
import NewQuestionView from '@/views/static/NewQuestionView.vue'

const turnstileConfig = vi.hoisted(() => ({ siteKey: '' }))

vi.mock('@/services/api/sectionService', () => ({
  getSections: vi.fn<() => Promise<Section[]>>(),
}))

vi.mock('@/services/api/questionService', () => ({
  createQuestion: vi.fn<typeof createQuestion>(),
}))

vi.mock('@/config/turnstile', () => ({
  TURNSTILE_ACTION: 'student_question',
  getTurnstileSiteKey: () => turnstileConfig.siteKey,
}))

const mockedGetSections = vi.mocked(getSections)
const mockedCreateQuestion = vi.mocked(createQuestion)
const createObjectUrl = vi.fn<(object: Blob) => string>(() => 'blob:preview')
const revokeObjectUrl = vi.fn<(url: string) => void>()
const turnstileReset = vi.fn<() => void>()

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>',
}

const turnstileWidgetStub = {
  props: ['siteKey', 'action'],
  emits: ['verified', 'expired', 'error'],
  methods: {
    reset: turnstileReset,
  },
  template: `
    <section data-testid="turnstile-widget">
      <button type="button" data-testid="turnstile-verify" @click="$emit('verified', 'valid-turnstile-token')">
        Verificar
      </button>
      <button type="button" data-testid="turnstile-expire" @click="$emit('expired')">Expirar</button>
      <button type="button" data-testid="turnstile-error" @click="$emit('error')">Error</button>
    </section>
  `,
}

const sections: Section[] = [
  {
    id: 1,
    type: 'TALLER',
    name: 'Taller 1',
    slug: 'taller-1',
    description: 'Morfometria de cuencas',
    displayOrder: 1,
  },
  {
    id: 4,
    type: 'PARCIAL',
    name: 'Parcial 1',
    slug: 'parcial-1',
    description: 'Parcial 1',
    displayOrder: 4,
  },
]

function mountView() {
  return mount(NewQuestionView, {
    global: {
      stubs: {
        RouterLink: routerLinkStub,
        TurnstileWidget: turnstileWidgetStub,
      },
    },
  })
}

async function mountLoadedView() {
  mockedGetSections.mockResolvedValue(sections)
  const wrapper = mountView()

  await flushPromises()

  return wrapper
}

describe('NewQuestionView', () => {
  beforeEach(() => {
    vi.unstubAllEnvs()
    turnstileConfig.siteKey = ''
    mockedGetSections.mockReset()
    mockedCreateQuestion.mockReset()
    turnstileReset.mockClear()
    createObjectUrl.mockClear()
    revokeObjectUrl.mockClear()
    vi.stubGlobal('URL', {
      createObjectURL: createObjectUrl,
      revokeObjectURL: revokeObjectUrl,
    })
    mockedCreateQuestion.mockResolvedValue({
      id: 1,
      status: 'PENDING',
      createdAt: '2026-01-01T00:00:00Z',
    })
  })

  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('shows loading while sections are requested', () => {
    mockedGetSections.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountView()

    expect(wrapper.text()).toContain('Cargando secciones...')
  })

  it('renders talleres and parciales returned by the API service', async () => {
    const wrapper = await mountLoadedView()

    expect(wrapper.text()).toContain('Taller 1')
    expect(wrapper.text()).toContain('Parcial 1')
  })

  it('validates an empty question before sending', async () => {
    const wrapper = await mountLoadedView()

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('Escribe tu pregunta.')
    expect(mockedCreateQuestion).not.toHaveBeenCalled()
  })

  it('allows an optional blank nickname and sends null', async () => {
    const wrapper = await mountLoadedView()

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('input#nickname').setValue('   ')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mockedCreateQuestion).toHaveBeenCalledWith({
      data: {
        sectionSlug: 'taller-1',
        nickname: null,
        question: 'Pregunta tecnica',
        turnstileToken: null,
      },
      image: null,
    })
  })

  it('sends the normalized payload when the form is valid', async () => {
    const wrapper = await mountLoadedView()

    await wrapper.get('select#section').setValue('parcial-1')
    await wrapper.get('input#nickname').setValue('  Estudiante  ')
    await wrapper.get('textarea#question').setValue('  Como reviso este procedimiento?  ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mockedCreateQuestion).toHaveBeenCalledWith({
      data: {
        sectionSlug: 'parcial-1',
        nickname: 'Estudiante',
        question: 'Como reviso este procedimiento?',
        turnstileToken: null,
      },
      image: null,
    })
  })

  it('validates a question that is too long', async () => {
    const wrapper = await mountLoadedView()

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('a'.repeat(QUESTION_MAX_LENGTH + 1))
    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain(`La pregunta debe tener ${QUESTION_MAX_LENGTH} caracteres o menos.`)
    expect(mockedCreateQuestion).not.toHaveBeenCalled()
  })

  it('keeps the submit button disabled while sending and prevents duplicate submits', async () => {
    mockedCreateQuestion.mockReturnValue(new Promise(() => undefined))
    const wrapper = await mountLoadedView()

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')
    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')

    const button = wrapper.get('button[type="submit"]')

    expect(button.attributes('disabled')).toBeDefined()
    expect(button.text()).toContain('Enviando pregunta...')
    expect(mockedCreateQuestion).toHaveBeenCalledTimes(1)
  })

  it('shows a success message after sending the question', async () => {
    const wrapper = await mountLoadedView()

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Tu pregunta fue enviada')
  })

  it('shows a friendly error when the request fails', async () => {
    mockedCreateQuestion.mockRejectedValue(new Error('Network error'))
    const wrapper = await mountLoadedView()

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos enviar tu pregunta.')
  })

  it('shows selected JPEG image name and preview', async () => {
    const wrapper = await mountLoadedView()
    const file = new File(['image'], 'captura.jpg', { type: 'image/jpeg' })

    await selectImage(wrapper, file)

    expect(wrapper.text()).toContain('captura.jpg')
    expect(createObjectUrl).toHaveBeenCalledWith(file)
    expect(wrapper.get('img').attributes('src')).toBe('blob:preview')
  })

  it('accepts a PNG image and sends it with the payload', async () => {
    const wrapper = await mountLoadedView()
    const file = new File(['image'], 'grafica.png', { type: 'image/png' })

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')
    await selectImage(wrapper, file)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mockedCreateQuestion).toHaveBeenCalledWith({
      data: {
        sectionSlug: 'taller-1',
        nickname: null,
        question: 'Pregunta tecnica',
        turnstileToken: null,
      },
      image: file,
    })
  })

  it('renders Turnstile when a site key is configured and blocks submit without a token', async () => {
    turnstileConfig.siteKey = 'test-site-key'
    const wrapper = await mountLoadedView()

    expect(wrapper.find('[data-testid="turnstile-widget"]').exists()).toBe(true)

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')

    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
    expect(mockedCreateQuestion).not.toHaveBeenCalled()
  })

  it('disables submission and does not mount Turnstile in preview read-only mode', async () => {
    vi.stubEnv('VITE_PREVIEW_READ_ONLY', 'true')
    turnstileConfig.siteKey = 'test-site-key'
    const wrapper = await mountLoadedView()

    expect(wrapper.text()).toContain('No disponible en la vista previa.')
    expect(wrapper.find('[data-testid="turnstile-widget"]').exists()).toBe(false)

    await wrapper.get('form').trigger('submit')

    expect(mockedCreateQuestion).not.toHaveBeenCalled()
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
  })

  it('sends the Turnstile token when the challenge is completed', async () => {
    turnstileConfig.siteKey = 'test-site-key'
    const wrapper = await mountLoadedView()

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')
    await wrapper.get('[data-testid="turnstile-verify"]').trigger('click')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mockedCreateQuestion).toHaveBeenCalledWith({
      data: {
        sectionSlug: 'taller-1',
        nickname: null,
        question: 'Pregunta tecnica',
        turnstileToken: 'valid-turnstile-token',
      },
      image: null,
    })
  })

  it('blocks submit again when the Turnstile token expires', async () => {
    turnstileConfig.siteKey = 'test-site-key'
    const wrapper = await mountLoadedView()

    await wrapper.get('[data-testid="turnstile-verify"]').trigger('click')
    await wrapper.get('[data-testid="turnstile-expire"]').trigger('click')

    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Completa nuevamente la verificación')
  })

  it('resets Turnstile after a successful submit', async () => {
    turnstileConfig.siteKey = 'test-site-key'
    const wrapper = await mountLoadedView()

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')
    await wrapper.get('[data-testid="turnstile-verify"]').trigger('click')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(turnstileReset).toHaveBeenCalled()
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
  })

  it('shows a Turnstile verification message when the backend rejects the challenge', async () => {
    turnstileConfig.siteKey = 'test-site-key'
    mockedCreateQuestion.mockRejectedValue({
      response: {
        status: 400,
        data: { message: 'Completa nuevamente la verificación y vuelve a intentarlo.' },
      },
    })
    const wrapper = await mountLoadedView()

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')
    await wrapper.get('[data-testid="turnstile-verify"]').trigger('click')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Completa nuevamente la verificación y vuelve a intentarlo.')
    expect(turnstileReset).toHaveBeenCalled()
  })

  it('shows a temporary verification error when Siteverify is unavailable', async () => {
    turnstileConfig.siteKey = 'test-site-key'
    mockedCreateQuestion.mockRejectedValue({
      response: {
        status: 503,
        data: { message: 'No pudimos verificar el envío en este momento. Intenta nuevamente.' },
      },
    })
    const wrapper = await mountLoadedView()

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')
    await wrapper.get('[data-testid="turnstile-verify"]').trigger('click')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('No pudimos verificar el envío en este momento. Intenta nuevamente.')
  })

  it('rejects an image larger than 5 MB before sending', async () => {
    const wrapper = await mountLoadedView()
    const file = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'grande.png', {
      type: 'image/png',
    })

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')
    await selectImage(wrapper, file)
    await wrapper.get('form').trigger('submit')

    expect(wrapper.text()).toContain('La imagen debe pesar 5 MB o menos.')
    expect(mockedCreateQuestion).not.toHaveBeenCalled()
  })

  it('rejects unsupported image types before sending', async () => {
    const wrapper = await mountLoadedView()
    const file = new File(['gif'], 'animacion.gif', { type: 'image/gif' })

    await selectImage(wrapper, file)

    expect(wrapper.text()).toContain('Adjunta una imagen JPEG o PNG.')
    expect(createObjectUrl).not.toHaveBeenCalled()
  })

  it('removes the selected image and revokes its preview', async () => {
    const wrapper = await mountLoadedView()
    const file = new File(['image'], 'grafica.png', { type: 'image/png' })

    await selectImage(wrapper, file)
    await wrapper.get('button[type="button"]').trigger('click')

    expect(wrapper.text()).not.toContain('grafica.png')
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:preview')
  })

  it('replaces a selected image and revokes the previous preview', async () => {
    createObjectUrl.mockReturnValueOnce('blob:first').mockReturnValueOnce('blob:second')
    const wrapper = await mountLoadedView()
    const firstFile = new File(['first'], 'primera.png', { type: 'image/png' })
    const secondFile = new File(['second'], 'segunda.png', { type: 'image/png' })

    await selectImage(wrapper, firstFile)
    await selectImage(wrapper, secondFile)

    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:first')
    expect(wrapper.text()).toContain('segunda.png')
    expect(wrapper.get('img').attributes('src')).toBe('blob:second')
  })

  it('revokes the selected image preview when the component is unmounted', async () => {
    const wrapper = await mountLoadedView()
    const file = new File(['image'], 'grafica.png', { type: 'image/png' })

    await selectImage(wrapper, file)
    wrapper.unmount()

    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:preview')
  })

  it('clears the image after success', async () => {
    const wrapper = await mountLoadedView()
    const file = new File(['image'], 'grafica.png', { type: 'image/png' })

    await wrapper.get('select#section').setValue('taller-1')
    await wrapper.get('textarea#question').setValue('Pregunta tecnica')
    await selectImage(wrapper, file)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Tu pregunta fue enviada')
    expect(wrapper.text()).not.toContain('grafica.png')
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:preview')
  })
})

async function selectImage(wrapper: ReturnType<typeof mount>, file: File) {
  const input = wrapper.get('input#image')
  Object.defineProperty(input.element, 'files', {
    value: [file],
    configurable: true,
  })

  await input.trigger('change')
}
