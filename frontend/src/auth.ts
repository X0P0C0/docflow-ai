import { reactive } from 'vue'
import { fetchCurrentUser, isDemoToken, type CurrentUser, type LoginResponse } from './api/auth'
import { isNetworkFallbackCandidate } from './api/http'

const TOKEN_KEY = 'docflow.ai.token'
const USER_KEY = 'docflow.ai.user'

function readUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as CurrentUser
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export const authState = reactive<{
  token: string
  user: CurrentUser | null
  restored: boolean
}>({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readUser(),
  restored: false,
})

let restorePromise: Promise<void> | null = null

export function isAuthenticated() {
  return Boolean(authState.token)
}

export async function restoreSession() {
  if (authState.restored) {
    return
  }

  if (!authState.token) {
    authState.restored = true
    return
  }

  if (isDemoToken(authState.token)) {
    authState.restored = true
    return
  }

  if (!restorePromise) {
    restorePromise = fetchCurrentUser()
      .then((user) => {
        updateCurrentUser(user)
        authState.restored = true
      })
      .catch((error) => {
        if (isNetworkFallbackCandidate(error) && authState.user) {
          authState.restored = true
          return
        }
        clearSession()
        authState.restored = true
      })
      .finally(() => {
        restorePromise = null
      })
  }

  await restorePromise
}

export function saveSession(session: LoginResponse) {
  authState.token = session.token
  authState.user = session.user
  authState.restored = true
  localStorage.setItem(TOKEN_KEY, session.token)
  localStorage.setItem(USER_KEY, JSON.stringify(session.user))
}

export function updateCurrentUser(user: CurrentUser) {
  authState.user = user
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  authState.token = ''
  authState.user = null
  authState.restored = true
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}
