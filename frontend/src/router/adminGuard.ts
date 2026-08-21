import type { NavigationGuardReturn } from 'vue-router'
import type { User } from 'firebase/auth'

import { getAdminMe } from '@/services/api/adminService'
import { observeAuthState, signOut } from '@/services/firebase/authService'

export async function requireAdmin(): Promise<NavigationGuardReturn> {
  const user = await waitForAuthState()

  if (!user) {
    return { name: 'admin-login' }
  }

  try {
    await getAdminMe()
    return true
  } catch {
    await signOut()

    return {
      name: 'admin-login',
      query: { reason: 'forbidden' },
    }
  }
}

function waitForAuthState(): Promise<User | null> {
  return new Promise((resolve) => {
    let unsubscribe: () => void = () => undefined
    unsubscribe = observeAuthState((user) => {
      unsubscribe()
      resolve(user)
    })
  })
}
