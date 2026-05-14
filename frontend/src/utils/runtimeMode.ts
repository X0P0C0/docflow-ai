import { isDemoToken } from '../api/auth'
import { authState } from '../auth'

export function isDemoMode() {
  return !authState.token || isDemoToken(authState.token)
}

export function getRuntimeModeText() {
  return isDemoMode() ? '演示模式' : '正常模式'
}
