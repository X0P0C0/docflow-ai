export interface ApiResponse<T> {
  code: number
  error?: string
  message: string
  data: T
  path?: string
  timestamp?: string
  errors?: ApiValidationError[]
  traceId?: string
}

interface RequestOptions extends RequestInit {
  skipAuth?: boolean
  skipGlobalAuthHandling?: boolean
}

export interface ApiValidationError {
  field?: string
  message: string
}

export interface ApiError extends Error {
  code?: number
  status?: number
  error?: string
  path?: string
  traceId?: string
  details?: ApiValidationError[]
}

type AuthFailureHandler = (error: ApiError) => void

let authFailureHandler: AuthFailureHandler | null = null

function getToken() {
  return localStorage.getItem('docflow.ai.token') || ''
}

async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')

  if (!options.skipAuth) {
    const token = getToken()
    if (token) {
      headers.set('Authorization', `Bearer ${token}`)
    }
  }

  const response = await fetch(url, {
    ...options,
    headers,
  })

  let result: ApiResponse<T> | null = null
  try {
    result = (await response.json()) as ApiResponse<T>
  } catch {
    result = null
  }

  if (!response.ok) {
    const error = new Error(result?.message || `Request failed with status ${response.status}`) as ApiError
    error.code = result?.code
    error.status = response.status
    error.error = result?.error
    error.path = result?.path
    error.traceId = result?.traceId
    error.details = result?.errors
    if (!options.skipAuth && !options.skipGlobalAuthHandling && (response.status === 401 || response.status === 403)) {
      authFailureHandler?.(error)
    }
    throw error
  }

  if (!result) {
    throw new Error('Empty response body')
  }

  if (result.code !== 200) {
    const error = new Error(result.message || 'Request failed') as ApiError
    error.code = result.code
    error.status = response.status
    error.error = result.error
    error.path = result.path
    error.traceId = result.traceId
    error.details = result.errors
    throw error
  }

  return result.data
}

export async function get<T>(url: string, options?: RequestOptions): Promise<T> {
  return request<T>(url, {
    method: 'GET',
    ...options,
  })
}

export async function post<T>(url: string, body?: unknown, options?: RequestOptions): Promise<T> {
  return request<T>(url, {
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body),
    ...options,
  })
}

export async function put<T>(url: string, body?: unknown, options?: RequestOptions): Promise<T> {
  return request<T>(url, {
    method: 'PUT',
    body: body === undefined ? undefined : JSON.stringify(body),
    ...options,
  })
}

export async function del<T>(url: string, options?: RequestOptions): Promise<T> {
  return request<T>(url, {
    method: 'DELETE',
    ...options,
  })
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof Error
}

export function setAuthFailureHandler(handler: AuthFailureHandler | null) {
  authFailureHandler = handler
}

export function isNetworkFallbackCandidate(error: unknown) {
  if (!isApiError(error)) {
    return false
  }
  if (typeof error.status !== 'number') {
    return true
  }
  return error.status >= 500
}

export function getApiErrorMessage(error: unknown, fallback = '请求失败，请稍后重试。') {
  if (!isApiError(error)) {
    return fallback
  }

  const traceSuffix = error.traceId ? `（追踪号：${error.traceId}）` : ''

  if (error.details?.length) {
    return `${error.details.map((item) => item.message).join('；')}${traceSuffix}`
  }

  return `${error.message || fallback}${traceSuffix}`
}

export function getApiErrorTraceId(error: unknown) {
  if (!isApiError(error)) {
    return ''
  }
  return error.traceId || ''
}
