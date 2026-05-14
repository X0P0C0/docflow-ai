export function getLoginAuthNotice(reason: unknown) {
  if (reason === 'session-expired') {
    return '登录状态已失效，请重新登录后继续。'
  }

  return ''
}
