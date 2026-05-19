import { reactive } from 'vue'
import { fetchCurrentUser, isDemoToken, type CurrentUser, type LoginResponse } from './api/auth'
import { isNetworkFallbackCandidate } from './api/http'
import { getSafeLocalStorage } from './utils/safeStorage'

const TOKEN_KEY = 'docflow.ai.token'
const USER_KEY = 'docflow.ai.user'
const storage = getSafeLocalStorage()

function readUser() {
  const raw = storage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as CurrentUser
  } catch {
    storage.removeItem(USER_KEY)
    return null
  }
}

export const authState = reactive<{
  token: string
  user: CurrentUser | null
  restored: boolean
}>({
  token: storage.getItem(TOKEN_KEY) || '',
  user: readUser(),
  restored: false,
})

// 避免首屏或路由切换时重复打多次“恢复当前会话”请求。
let restorePromise: Promise<void> | null = null

export function isAuthenticated() {
  return Boolean(authState.token)
}

export async function restoreSession() {
  // restored 的目标不是证明“已经登录”，而是证明“当前会话状态已经判断完成”。
  if (authState.restored) {
    return
  }

  if (!authState.token) {
    authState.restored = true
    return
  }

  if (isDemoToken(authState.token)) {
    // demo token 不依赖后端校验，恢复本地态即可。
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
        // 如果只是后端暂时不可用，但本地还有用户信息，就先保留可演示状态。
        if (isNetworkFallbackCandidate(error) && authState.user) {
          authState.restored = true
          return
        }
        // 其余情况按“会话已经失效”处理，避免后续页面带着脏 token 继续跑。
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
  // 登录成功后同时更新内存态和 localStorage，保证刷新页面后还能恢复会话。
  authState.token = session.token
  authState.user = session.user
  authState.restored = true
  storage.setItem(TOKEN_KEY, session.token)
  storage.setItem(USER_KEY, JSON.stringify(session.user))
}

export function updateCurrentUser(user: CurrentUser) {
  authState.user = user
  storage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  // 退出或 token 失效时统一从这里清理，避免不同页面各自删一部分状态。
  // 清理时顺手把 restored 置回 true，避免路由守卫再次等待恢复流程。
  authState.token = ''
  authState.user = null
  authState.restored = true
  storage.removeItem(TOKEN_KEY)
  storage.removeItem(USER_KEY)
}
