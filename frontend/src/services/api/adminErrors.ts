import axios from 'axios'

export function isAdminAuthorizationError(error: unknown): boolean {
  if (!axios.isAxiosError(error)) {
    return false
  }

  return error.response?.status === 401 || error.response?.status === 403
}
