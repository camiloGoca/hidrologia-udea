<script setup lang="ts">
import { onMounted, ref } from 'vue'

import AppFooter from '@/components/AppFooter.vue'
import AppHeader from '@/components/AppHeader.vue'
import { getPublicVisitCount, recordSiteVisit } from '@/services/api/analyticsService'

const siteVisits = ref<number | null>(null)

onMounted(() => {
  void initializeAnalytics()
})

async function initializeAnalytics() {
  try {
    await recordSiteVisit()
  } catch {
    // Analytics must never block the public experience.
  }

  try {
    siteVisits.value = (await getPublicVisitCount()).visits
  } catch {
    siteVisits.value = null
  }
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-950">
    <AppHeader />

    <main>
      <RouterView />
    </main>

    <AppFooter :site-visits="siteVisits" />
  </div>
</template>
