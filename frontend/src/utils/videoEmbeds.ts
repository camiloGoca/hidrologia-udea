import type { PostContentVideoProvider } from '@/types/postContent'

export interface ParsedVideoEmbed {
  provider: PostContentVideoProvider
  sourceUrl: string
  videoId: string | null
}

export interface VideoParseFailure {
  message: string
}

export type VideoParseResult =
  | { ok: true; value: ParsedVideoEmbed }
  | { ok: false; error: VideoParseFailure }

const YOUTUBE_HOSTS = new Set(['youtube.com', 'www.youtube.com', 'm.youtube.com', 'youtu.be'])
const TIKTOK_HOSTS = new Set(['tiktok.com', 'www.tiktok.com', 'm.tiktok.com'])
const TIKTOK_SHORT_HOSTS = new Set(['vm.tiktok.com', 'vt.tiktok.com'])
const YOUTUBE_ID_PATTERN = /^[A-Za-z0-9_-]{6,64}$/
const TIKTOK_ID_PATTERN = /^\d{5,30}$/
const MAX_SOURCE_URL_LENGTH = 2048

export function parseVideoUrl(rawUrl: string): VideoParseResult {
  const sourceUrl = rawUrl.trim()
  if (!sourceUrl || sourceUrl.length > MAX_SOURCE_URL_LENGTH) {
    return genericVideoError()
  }

  let url: URL
  try {
    url = new URL(sourceUrl)
  } catch {
    return genericVideoError()
  }

  if (url.protocol !== 'https:') {
    return genericVideoError()
  }

  const host = url.hostname.toLowerCase()
  if (TIKTOK_SHORT_HOSTS.has(host)) {
    return {
      ok: false,
      error: { message: 'Usa el enlace completo del video de TikTok.' },
    }
  }

  if (YOUTUBE_HOSTS.has(host)) {
    return parseYoutubeUrl(url, sourceUrl)
  }

  if (TIKTOK_HOSTS.has(host)) {
    return parseTiktokUrl(url, sourceUrl)
  }

  if (isDirectVideoUrl(url)) {
    return {
      ok: true,
      value: {
        provider: 'direct',
        sourceUrl,
        videoId: null,
      },
    }
  }

  return genericVideoError()
}

export function youtubeEmbedUrl(videoId: string | null | undefined): string | null {
  return isYoutubeVideoId(videoId) ? `https://www.youtube.com/embed/${videoId}` : null
}

export function tiktokEmbedUrl(videoId: string | null | undefined): string | null {
  return isTiktokVideoId(videoId) ? `https://www.tiktok.com/player/v1/${videoId}` : null
}

export function isDirectVideoSource(sourceUrl: string | null | undefined): boolean {
  if (!sourceUrl) {
    return false
  }

  try {
    const url = new URL(sourceUrl)

    return url.protocol === 'https:' && isDirectVideoUrl(url)
  } catch {
    return false
  }
}

function parseYoutubeUrl(url: URL, sourceUrl: string): VideoParseResult {
  const videoId = extractYoutubeVideoId(url)
  if (!isYoutubeVideoId(videoId)) {
    return genericVideoError()
  }

  return {
    ok: true,
    value: {
      provider: 'youtube',
      sourceUrl,
      videoId,
    },
  }
}

function parseTiktokUrl(url: URL, sourceUrl: string): VideoParseResult {
  const videoId = extractTiktokVideoId(url)
  if (!isTiktokVideoId(videoId)) {
    return genericVideoError()
  }

  return {
    ok: true,
    value: {
      provider: 'tiktok',
      sourceUrl,
      videoId,
    },
  }
}

function extractYoutubeVideoId(url: URL): string | null {
  const segments = pathSegments(url)
  if (url.hostname.toLowerCase() === 'youtu.be') {
    return segments.length === 1 ? (segments[0] ?? null) : null
  }

  if (url.pathname === '/watch') {
    return url.searchParams.get('v')
  }

  if (segments.length === 2 && segments[0] === 'shorts') {
    return segments[1] ?? null
  }

  if (segments.length === 2 && segments[0] === 'embed') {
    return segments[1] ?? null
  }

  if (segments.length === 2 && segments[0] === 'live') {
    return segments[1] ?? null
  }

  return null
}

function extractTiktokVideoId(url: URL): string | null {
  const segments = pathSegments(url)
  if (
    segments.length === 3 &&
    segments[0]?.startsWith('@') &&
    segments[1] === 'video'
  ) {
    return segments[2] ?? null
  }

  if (segments.length === 3 && segments[0] === 'player' && segments[1] === 'v1') {
    return segments[2] ?? null
  }

  return null
}

function pathSegments(url: URL): string[] {
  return url.pathname.replace(/^\/+/, '').split('/')
}

function isYoutubeVideoId(value: string | null | undefined): value is string {
  return typeof value === 'string' && YOUTUBE_ID_PATTERN.test(value)
}

function isTiktokVideoId(value: string | null | undefined): value is string {
  return typeof value === 'string' && TIKTOK_ID_PATTERN.test(value)
}

function isDirectVideoUrl(url: URL): boolean {
  const pathname = url.pathname.toLowerCase()

  return pathname.endsWith('.mp4') || pathname.endsWith('.webm')
}

function genericVideoError(): VideoParseResult {
  return {
    ok: false,
    error: { message: 'Usa un enlace de YouTube, TikTok o un archivo HTTPS .mp4/.webm.' },
  }
}
