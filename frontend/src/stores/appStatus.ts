import { defineStore } from 'pinia'

export const useAppStatusStore = defineStore('appStatus', {
  state: () => ({
    frontendReady: true,
  }),
})
