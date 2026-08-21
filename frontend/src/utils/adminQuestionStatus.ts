import type { AdminQuestionStatus } from '@/types/adminQuestion'

export const ADMIN_QUESTION_STATUS_LABELS: Record<AdminQuestionStatus, string> = {
  PENDING: 'PENDIENTE',
  ARCHIVED: 'ARCHIVADA',
  REJECTED: 'RECHAZADA',
  PUBLISHED: 'PUBLICADA',
}

export function adminQuestionStatusLabel(status: AdminQuestionStatus): string {
  return ADMIN_QUESTION_STATUS_LABELS[status]
}
