import { describe, expect, it } from 'vitest'

import { parseVideoUrl } from '@/utils/videoEmbeds'

describe('videoEmbeds', () => {
  it('parses supported YouTube URLs', () => {
    for (const url of [
      'https://www.youtube.com/watch?v=abc_DEF1234',
      'https://youtu.be/abc_DEF1234',
      'https://www.youtube.com/shorts/abc_DEF1234',
      'https://www.youtube.com/embed/abc_DEF1234',
    ]) {
      expect(parseVideoUrl(url)).toEqual({
        ok: true,
        value: {
          provider: 'youtube',
          sourceUrl: url,
          videoId: 'abc_DEF1234',
        },
      })
    }
  })

  it('parses TikTok canonical and player URLs', () => {
    expect(parseVideoUrl('https://www.tiktok.com/@udea/video/1234567890')).toEqual({
      ok: true,
      value: {
        provider: 'tiktok',
        sourceUrl: 'https://www.tiktok.com/@udea/video/1234567890',
        videoId: '1234567890',
      },
    })
    expect(parseVideoUrl('https://www.tiktok.com/player/v1/1234567890')).toEqual({
      ok: true,
      value: {
        provider: 'tiktok',
        sourceUrl: 'https://www.tiktok.com/player/v1/1234567890',
        videoId: '1234567890',
      },
    })
  })

  it('parses direct HTTPS mp4 and webm URLs', () => {
    expect(parseVideoUrl('https://cdn.example.edu/video.mp4')).toMatchObject({
      ok: true,
      value: {
        provider: 'direct',
        sourceUrl: 'https://cdn.example.edu/video.mp4',
        videoId: null,
      },
    })
    expect(parseVideoUrl('https://cdn.example.edu/video.webm')).toMatchObject({
      ok: true,
      value: {
        provider: 'direct',
        sourceUrl: 'https://cdn.example.edu/video.webm',
        videoId: null,
      },
    })
  })

  it('rejects generic, unsafe and short TikTok URLs with useful messages', () => {
    expect(parseVideoUrl('https://example.com/pagina-del-video')).toEqual({
      ok: false,
      error: { message: 'Usa un enlace de YouTube, TikTok o un archivo HTTPS .mp4/.webm.' },
    })
    expect(parseVideoUrl('javascript:alert(1)')).toEqual({
      ok: false,
      error: { message: 'Usa un enlace de YouTube, TikTok o un archivo HTTPS .mp4/.webm.' },
    })
    expect(parseVideoUrl('https://vm.tiktok.com/ZM123/')).toEqual({
      ok: false,
      error: { message: 'Usa el enlace completo del video de TikTok.' },
    })
  })

  it('rejects unsupported TikTok path shapes', () => {
    for (const url of [
      'https://www.tiktok.com/cualquier-cosa/video/123456789',
      'https://www.tiktok.com/video/123456789',
      'https://www.tiktok.com/@usuario/otro/123456789',
      'https://www.tiktok.com/@usuario/video/123456789/extra',
      'https://www.tiktok.com/player/v1/123456789/extra',
    ]) {
      expect(parseVideoUrl(url)).toEqual({
        ok: false,
        error: { message: 'Usa un enlace de YouTube, TikTok o un archivo HTTPS .mp4/.webm.' },
      })
    }
  })
})
