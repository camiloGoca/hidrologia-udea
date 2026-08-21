import { beforeEach, describe, expect, it, vi } from 'vitest'

const firebaseMocks = vi.hoisted(() => {
  const auth = { currentUser: null as { getIdToken: () => Promise<string> } | null }

  return {
    auth,
    browserSessionPersistence: { type: 'SESSION' },
    getAuth: vi.fn<() => typeof auth>(() => auth),
    onAuthStateChanged: vi.fn<(callback: (user: unknown) => void) => () => void>(),
    setPersistence: vi.fn<() => Promise<void>>(() => Promise.resolve()),
    signInWithEmailAndPassword: vi.fn<() => Promise<{ user: unknown }>>(),
    firebaseSignOut: vi.fn<() => Promise<void>>(() => Promise.resolve()),
  }
})

vi.mock('@/services/firebase/firebaseApp', () => ({
  getFirebaseApp: vi.fn<() => { name: string }>(() => ({ name: 'test-app' })),
}))

vi.mock('firebase/auth', () => ({
  browserSessionPersistence: firebaseMocks.browserSessionPersistence,
  getAuth: firebaseMocks.getAuth,
  onAuthStateChanged: firebaseMocks.onAuthStateChanged,
  setPersistence: firebaseMocks.setPersistence,
  signInWithEmailAndPassword: firebaseMocks.signInWithEmailAndPassword,
  signOut: firebaseMocks.firebaseSignOut,
}))

describe('authService', () => {
  beforeEach(() => {
    vi.resetModules()
    firebaseMocks.auth.currentUser = null
    firebaseMocks.getAuth.mockClear()
    firebaseMocks.onAuthStateChanged.mockReset()
    firebaseMocks.setPersistence.mockClear()
    firebaseMocks.signInWithEmailAndPassword.mockReset()
    firebaseMocks.firebaseSignOut.mockClear()
  })

  it('configures browser session persistence before sign in', async () => {
    const user = { uid: 'admin-uid' }
    firebaseMocks.signInWithEmailAndPassword.mockResolvedValue({ user })
    const { signIn } = await import('@/services/firebase/authService')

    await expect(signIn('profesor@example.com', 'secret')).resolves.toBe(user)

    expect(firebaseMocks.setPersistence).toHaveBeenCalledWith(
      firebaseMocks.auth,
      firebaseMocks.browserSessionPersistence,
    )
    expect(firebaseMocks.signInWithEmailAndPassword).toHaveBeenCalledWith(
      firebaseMocks.auth,
      'profesor@example.com',
      'secret',
    )
  })

  it('returns the current user id token when available', async () => {
    firebaseMocks.auth.currentUser = { getIdToken: vi.fn<() => Promise<string>>(() => Promise.resolve('id-token')) }
    const { getIdToken } = await import('@/services/firebase/authService')

    await expect(getIdToken()).resolves.toBe('id-token')
  })

  it('returns null id token when there is no current user', async () => {
    const { getIdToken } = await import('@/services/firebase/authService')

    await expect(getIdToken()).resolves.toBeNull()
  })

  it('signs out through Firebase Auth', async () => {
    const { signOut } = await import('@/services/firebase/authService')

    await signOut()

    expect(firebaseMocks.firebaseSignOut).toHaveBeenCalledWith(firebaseMocks.auth)
  })
})
