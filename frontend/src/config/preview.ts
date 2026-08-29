export function isPreviewReadOnlyMode() {
  return import.meta.env.VITE_PREVIEW_READ_ONLY === 'true'
}
