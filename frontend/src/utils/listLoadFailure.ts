import { getApiErrorMessage, getApiErrorTraceId, isNetworkFallbackCandidate } from '../api/http'

export interface ListLoadFailureResult {
  shouldUseFallbackData: boolean
  message: string
  traceId: string
}

export function resolveListLoadFailure(
  error: unknown,
  options: {
    networkFallbackMessage: string
    defaultMessage: string
  },
): ListLoadFailureResult {
  if (isNetworkFallbackCandidate(error)) {
    return {
      shouldUseFallbackData: true,
      message: options.networkFallbackMessage,
      traceId: '',
    }
  }

  return {
    shouldUseFallbackData: false,
    message: getApiErrorMessage(error, options.defaultMessage),
    traceId: getApiErrorTraceId(error),
  }
}
