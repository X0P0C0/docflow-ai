import { isNetworkFallbackCandidate } from '../api/http'
import { getApiErrorDisplay, type ApiErrorDisplay } from './apiErrorDisplay'

export interface TicketDetailFallbackResult extends ApiErrorDisplay {
  mode: 'show-error' | 'fallback'
}

export function resolveTicketDetailFallback(
  error: unknown,
  defaultMessage: string,
): TicketDetailFallbackResult {
  if (isNetworkFallbackCandidate(error)) {
    return {
      mode: 'fallback',
      message: '',
      traceId: '',
    }
  }

  return {
    mode: 'show-error',
    ...getApiErrorDisplay(error, defaultMessage),
  }
}
