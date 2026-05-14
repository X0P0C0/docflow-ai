import { getApiErrorMessage, getApiErrorTraceId, isNetworkFallbackCandidate } from '../api/http'

export interface TicketSubmissionFailureResult {
  mode: 'show-error' | 'fallback-local'
  message: string
  traceId: string
}

export function resolveTicketSubmissionFailure(error: unknown): TicketSubmissionFailureResult {
  if (isNetworkFallbackCandidate(error)) {
    return {
      mode: 'fallback-local',
      message: '',
      traceId: '',
    }
  }

  return {
    mode: 'show-error',
    message: getApiErrorMessage(error, '工单提交失败，请稍后重试。'),
    traceId: getApiErrorTraceId(error),
  }
}
