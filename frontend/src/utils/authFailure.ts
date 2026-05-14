import type { ApiError } from '../api/http'

export function buildAuthFailureRedirect(currentPath: string, error: Pick<ApiError, 'status'>) {
  if (error.status === 401) {
    return {
      path: '/login',
      query: {
        redirect: currentPath === '/login' ? undefined : currentPath,
        reason: 'session-expired',
      },
    }
  }

  return {
    path: '/dashboard',
    query: {
      reason: 'forbidden',
      from: currentPath === '/dashboard' ? undefined : currentPath,
    },
  }
}

export function shouldSkipAuthFailureRedirect(
  currentRoute: {
    path: string
    query?: Record<string, unknown>
  },
  error: Pick<ApiError, 'status'>,
) {
  if (error.status === 401 && currentRoute.path === '/login') {
    return true
  }

  if (error.status === 403 && currentRoute.query?.reason === 'forbidden') {
    return true
  }

  return false
}
