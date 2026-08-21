import {
  browserSessionPersistence,
  getAuth,
  onAuthStateChanged,
  setPersistence,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  type Auth,
  type Unsubscribe,
  type User,
} from 'firebase/auth'

import { getFirebaseApp } from './firebaseApp'

let persistencePromise: Promise<void> | null = null

export function getFirebaseAuth(): Auth {
  return getAuth(getFirebaseApp())
}

export async function signIn(email: string, password: string): Promise<User> {
  const auth = getFirebaseAuth()
  await ensureSessionPersistence(auth)
  const credential = await signInWithEmailAndPassword(auth, email, password)

  return credential.user
}

export async function signOut(): Promise<void> {
  await firebaseSignOut(getFirebaseAuth())
}

export function getCurrentUser(): User | null {
  return getFirebaseAuth().currentUser
}

export async function getIdToken(): Promise<string | null> {
  const user = getCurrentUser()

  return user ? user.getIdToken() : null
}

export function observeAuthState(callback: (user: User | null) => void): Unsubscribe {
  return onAuthStateChanged(getFirebaseAuth(), callback)
}

function ensureSessionPersistence(auth: Auth): Promise<void> {
  persistencePromise ??= setPersistence(auth, browserSessionPersistence)

  return persistencePromise
}
