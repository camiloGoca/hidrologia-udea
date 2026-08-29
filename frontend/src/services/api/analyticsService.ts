import { httpClient } from './httpClient'

import { isPreviewReadOnlyMode } from '@/config/preview'
import type { AnalyticsSessionRequest, PublicVisitCount } from '@/types/analytics'

const ANALYTICS_SESSION_KEY = 'hidrologia-udea.analytics.sessionId'

export function getOrCreateAnalyticsSessionId(): string {
  const existing = sessionStorage.getItem(ANALYTICS_SESSION_KEY)
  if (existing) {
    return existing
  }

  const sessionId = createSessionId()
  sessionStorage.setItem(ANALYTICS_SESSION_KEY, sessionId)

  return sessionId
}

export async function recordSiteVisit(): Promise<void> {
  if (isPreviewReadOnlyMode()) {
    return
  }

  await httpClient.post('/analytics/visit', sessionPayload())
}

export async function recordSectionView(slug: string): Promise<void> {
  if (isPreviewReadOnlyMode()) {
    return
  }

  await httpClient.post(`/analytics/sections/${slug}/view`, sessionPayload())
}

export async function recordPostView(postId: number | string): Promise<void> {
  if (isPreviewReadOnlyMode()) {
    return
  }

  await httpClient.post(`/analytics/posts/${postId}/view`, sessionPayload())
}

export async function getPublicVisitCount(): Promise<PublicVisitCount> {
  const response = await httpClient.get<PublicVisitCount>('/analytics/visits/count')

  return response.data
}

function sessionPayload(): AnalyticsSessionRequest {
  return { sessionId: getOrCreateAnalyticsSessionId() }
}

function createSessionId(): string {
  const browserCrypto = globalThis.crypto

  if (typeof browserCrypto.randomUUID === 'function') {
    return browserCrypto.randomUUID()
  }

  const randomValues = browserCrypto.getRandomValues(new Uint8Array(16))
  randomValues[6] = (randomValues[6]! & 0x0f) | 0x40
  randomValues[8] = (randomValues[8]! & 0x3f) | 0x80
  const hex = [...randomValues].map((value) => value.toString(16).padStart(2, '0'))

  return `${hex.slice(0, 4).join('')}-${hex.slice(4, 6).join('')}-${hex.slice(6, 8).join('')}-${hex.slice(8, 10).join('')}-${hex.slice(10).join('')}`
}
