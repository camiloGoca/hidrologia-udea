export const TURNSTILE_ACTION = 'student_question'

export function getTurnstileSiteKey() {
  return import.meta.env.VITE_TURNSTILE_SITE_KEY || ''
}
