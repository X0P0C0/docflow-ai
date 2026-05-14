import { getApiErrorMessage, getApiErrorTraceId, isNetworkFallbackCandidate } from '../api/http'

export interface KnowledgeEditorFailureResult {
  mode: 'show-error' | 'fallback-local'
  message: string
  traceId: string
}

export function resolveKnowledgeEditorLoadFailure(error: unknown): KnowledgeEditorFailureResult {
  if (isNetworkFallbackCandidate(error)) {
    return {
      mode: 'fallback-local',
      message: '远程文章加载失败，当前可以继续新建本地草稿。',
      traceId: '',
    }
  }

  return {
    mode: 'show-error',
    message: getApiErrorMessage(error, '文章加载失败，请稍后重试。'),
    traceId: getApiErrorTraceId(error),
  }
}

export function resolveKnowledgeEditorSaveFailure(error: unknown, status: number): KnowledgeEditorFailureResult {
  if (isNetworkFallbackCandidate(error)) {
    return {
      mode: 'fallback-local',
      message: status === 1 ? '后端不可用，已回退为本地发布预览。' : '后端不可用，草稿已保存到本地。',
      traceId: '',
    }
  }

  return {
    mode: 'show-error',
    message: getApiErrorMessage(error, '知识文章保存失败，请稍后重试。'),
    traceId: getApiErrorTraceId(error),
  }
}
