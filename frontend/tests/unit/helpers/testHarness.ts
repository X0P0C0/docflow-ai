import { getSafeLocalStorage, getSafeSessionStorage } from '../../../src/utils/safeStorage'

export function resetWebStorage() {
  getSafeLocalStorage().clear()
  getSafeSessionStorage().clear()
}

export function assignRouteState<T extends Record<string, unknown>>(route: T, patch: Partial<T>) {
  Object.assign(route, patch)
}
