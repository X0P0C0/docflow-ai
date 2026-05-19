import { isDemoToken } from '../api/auth'
import { authState } from '../auth'

export type RuntimeMode = 'demo' | 'live'

export function getRuntimeMode(): RuntimeMode {
  return !authState.token || isDemoToken(authState.token) ? 'demo' : 'live'
}

export function isDemoMode() {
  return getRuntimeMode() === 'demo'
}

export function getRuntimeModeText() {
  return isDemoMode() ? '演示模式' : '真实模式'
}

export function getRuntimeModeHeadline() {
  return isDemoMode() ? '当前正在使用演示会话' : '当前正在使用真实后端会话'
}

export function getRuntimeEntryMessage(subject = '当前入口') {
  if (isDemoMode()) {
    return `当前${subject}默认会进入演示模式，登录后会优先使用本地演示数据来预览页面和流程。`
  }

  return `当前${subject}默认会进入真实模式，登录后会优先读取并写入真实后端数据。`
}

export function getRuntimeDataSourceMessage(options: {
  usedFallbackData: boolean
  subject: string
  localOnlyLabel?: string
}) {
  if (isDemoMode()) {
    return `当前是演示模式，${options.subject}优先展示本地演示数据。`
  }

  if (options.usedFallbackData) {
    return `真实接口暂时不可用，当前${options.subject}已回退到兜底数据。`
  }

  if (options.localOnlyLabel) {
    return `当前${options.subject}优先展示真实接口结果，本地${options.localOnlyLabel}会作为补充一起显示。`
  }

  return `当前${options.subject}正在使用真实接口数据。`
}
