import axios, { AxiosHeaders } from 'axios'

import { getIdToken } from '@/services/firebase/authService'

export const adminHttpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: {
    Accept: 'application/json',
  },
})

adminHttpClient.interceptors.request.use(async (config) => {
  const token = await getIdToken()

  if (token) {
    const headers = AxiosHeaders.from(config.headers)
    headers.set('Authorization', `Bearer ${token}`)
    config.headers = headers
  }

  return config
})
