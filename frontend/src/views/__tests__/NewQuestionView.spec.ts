import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { createQuestion } from '@/services/api/questionService'
import { getSections } from '@/services/api/sectionService'
import type { Section } from '@/types/section'
import { QUESTION_MAX_LENGTH } from '@/types/studentQuestion'
import NewQuestionView from '@/views/static/NewQuestionView.vue'

vi.mock('@/services/api/sectionService', () => ({
  getSections: vi.fn<() => Promise<Section[]>>(),
}))

vi.mock('@/services/api/questionService', () => ({
  createQuestion: vi.fn<typeof createQuestion>(),
}))

const mockedGetSections = vi.mocked(getSections)
const mockedCreateQuestion = vi.mocked(createQuestion)

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>',
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
      stubs: { RouterLink: routerLinkStub },
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
    mockedGetSections.mockReset()
    mockedCreateQuestion.mockReset()
    mockedCreateQuestion.mockResolvedValue({
      id: 1,
      status: 'PENDING',
      createdAt: '2026-01-01T00:00:00Z',
    })
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
      sectionSlug: 'taller-1',
      nickname: null,
      question: 'Pregunta tecnica',
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
      sectionSlug: 'parcial-1',
      nickname: 'Estudiante',
      question: 'Como reviso este procedimiento?',
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
})
