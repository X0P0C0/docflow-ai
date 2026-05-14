export function resetWebStorage() {
  localStorage.clear()
  sessionStorage.clear()
}

export function assignRouteState<T extends Record<string, unknown>>(route: T, patch: Partial<T>) {
  Object.assign(route, patch)
}
