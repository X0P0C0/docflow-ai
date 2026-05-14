import { getApiErrorMessage, getApiErrorTraceId } from '../api/http'

export interface ApiErrorDisplay {
  message: string
  traceId: string
}

export function getApiErrorDisplay(error: unknown, defaultMessage: string): ApiErrorDisplay {
  return {
    message: getApiErrorMessage(error, defaultMessage),
    traceId: getApiErrorTraceId(error),
  }
}
