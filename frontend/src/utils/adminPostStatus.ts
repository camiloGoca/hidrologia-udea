import type { AdminPostStatus } from '@/types/adminPost'

export const ADMIN_POST_STATUS_LABELS: Record<AdminPostStatus, string> = {
  DRAFT: 'BORRADOR',
  PUBLISHED: 'PUBLICADA',
  ARCHIVED: 'ARCHIVADA',
}

export function adminPostStatusLabel(status: AdminPostStatus): string {
  return ADMIN_POST_STATUS_LABELS[status]
}
