import { computed, onMounted, ref } from 'vue'

import { getSections } from '@/services/api/sectionService'
import type { Section, SectionType } from '@/types/section'

export function useSections() {
  const sections = ref<Section[]>([])
  const isLoading = ref(true)
  const hasError = ref(false)

  const isEmpty = computed(
    () => !isLoading.value && !hasError.value && sections.value.length === 0,
  )

  async function loadSections() {
    isLoading.value = true
    hasError.value = false

    try {
      sections.value = await getSections()
    } catch {
      hasError.value = true
      sections.value = []
    } finally {
      isLoading.value = false
    }
  }

  function sectionsByType(type: SectionType) {
    return computed(() => sections.value.filter((section) => section.type === type))
  }

  onMounted(loadSections)

  return {
    sections,
    isLoading,
    hasError,
    isEmpty,
    loadSections,
    sectionsByType,
  }
}
