import { getSafeLocalStorage } from '../utils/safeStorage'

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
const storage = getSafeLocalStorage()

function getToken() {
  return storage.getItem('docflow.ai.token') || ''
}

async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  // 所有前端 API 最终都收口到这里：
  // 统一补 token、统一解析 ApiResponse、统一把 401/403 交给全局路由处理。
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
    // 允许后端在异常场景下返回空体，这里统一走后面的错误分支处理。
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
    // 401/403 的统一跳转不写死在这里，交给 router 注入的 handler 处理。
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
  // 这里不直接依赖 router，避免 http 基础层和路由层互相反向耦合。
  authFailureHandler = handler
}

export function isNetworkFallbackCandidate(error: unknown) {
  // 这个判断专门服务“真实接口失败时是否允许页面回退到本地演示/兜底数据”。
  if (!isApiError(error)) {
    return false
  }
  if (typeof error.status !== 'number') {
    // 连状态码都拿不到，通常说明请求根本没打通，可以考虑 fallback。
    return true
  }
  // 5xx 更像服务端暂时不可用，不一定要立刻把本地演示态也一起清掉。
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
