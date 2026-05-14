const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const distRoot = path.join(__dirname, '..', '.test-dist')
const packageJsonPath = path.join(distRoot, 'package.json')

if (!fs.existsSync(packageJsonPath)) {
  fs.mkdirSync(distRoot, { recursive: true })
  fs.writeFileSync(packageJsonPath, JSON.stringify({ type: 'commonjs' }, null, 2))
}

const http = require('../.test-dist/src/api/http.js')
const apiErrorDisplay = require('../.test-dist/src/utils/apiErrorDisplay.js')
const authFailure = require('../.test-dist/src/utils/authFailure.js')
const knowledgeEditor = require('../.test-dist/src/utils/knowledgeEditor.js')
const listLoadFailure = require('../.test-dist/src/utils/listLoadFailure.js')
const loginAuthNotice = require('../.test-dist/src/utils/loginAuthNotice.js')
const ticketDetailFailure = require('../.test-dist/src/utils/ticketDetailFailure.js')
const ticketSubmission = require('../.test-dist/src/utils/ticketSubmission.js')

function createApiError(overrides = {}) {
  return Object.assign(new Error('mock error'), overrides)
}

function test(name, fn) {
  try {
    fn()
    console.log(`PASS ${name}`)
  } catch (error) {
    console.error(`FAIL ${name}`)
    throw error
  }
}

test('isNetworkFallbackCandidate returns true for network-like errors without status', () => {
  assert.equal(http.isNetworkFallbackCandidate(createApiError()), true)
})

test('isNetworkFallbackCandidate returns true for 5xx errors', () => {
  assert.equal(http.isNetworkFallbackCandidate(createApiError({ status: 503 })), true)
})

test('isNetworkFallbackCandidate returns false for business 4xx errors', () => {
  assert.equal(http.isNetworkFallbackCandidate(createApiError({ status: 422 })), false)
  assert.equal(http.isNetworkFallbackCandidate(createApiError({ status: 403 })), false)
})

test('getApiErrorMessage appends traceId to validation details', () => {
  const error = createApiError({
    details: [
      { message: '标题不能为空' },
      { message: '描述不能为空' },
    ],
    traceId: 'trace-422',
  })

  assert.equal(
    http.getApiErrorMessage(error, 'fallback'),
    '标题不能为空；描述不能为空（追踪号：trace-422）',
  )
})

test('getApiErrorMessage appends traceId to plain error messages', () => {
  const error = createApiError({
    message: '无权访问该资源',
    traceId: 'trace-403',
  })

  assert.equal(
    http.getApiErrorMessage(error, 'fallback'),
    '无权访问该资源（追踪号：trace-403）',
  )
})

test('getApiErrorTraceId returns empty string for non-api errors', () => {
  assert.equal(http.getApiErrorTraceId({ message: 'plain object' }), '')
})

test('buildAuthFailureRedirect sends 401 users back to login with redirect', () => {
  assert.deepEqual(
    authFailure.buildAuthFailureRedirect('/tickets/15', { status: 401 }),
    {
      path: '/login',
      query: {
        redirect: '/tickets/15',
        reason: 'session-expired',
      },
    },
  )
})

test('buildAuthFailureRedirect sends 403 users to dashboard with source route', () => {
  assert.deepEqual(
    authFailure.buildAuthFailureRedirect('/knowledge/articles/create', { status: 403 }),
    {
      path: '/dashboard',
      query: {
        reason: 'forbidden',
        from: '/knowledge/articles/create',
      },
    },
  )
})

test('shouldSkipAuthFailureRedirect suppresses duplicate login and forbidden redirects', () => {
  assert.equal(
    authFailure.shouldSkipAuthFailureRedirect({ path: '/login', query: {} }, { status: 401 }),
    true,
  )
  assert.equal(
    authFailure.shouldSkipAuthFailureRedirect({ path: '/dashboard', query: { reason: 'forbidden' } }, { status: 403 }),
    true,
  )
  assert.equal(
    authFailure.shouldSkipAuthFailureRedirect({ path: '/tickets', query: {} }, { status: 403 }),
    false,
  )
})

test('getLoginAuthNotice returns session-expired prompt for login page', () => {
  assert.equal(
    loginAuthNotice.getLoginAuthNotice('session-expired'),
    '登录状态已失效，请重新登录后继续。',
  )
  assert.equal(loginAuthNotice.getLoginAuthNotice('forbidden'), '')
})

test('resolveTicketSubmissionFailure keeps business errors on the form instead of falling back', () => {
  const result = ticketSubmission.resolveTicketSubmissionFailure(createApiError({
    status: 422,
    message: '标题长度超过限制',
    traceId: 'trace-422-ticket',
  }))

  assert.deepEqual(result, {
    mode: 'show-error',
    message: '标题长度超过限制（追踪号：trace-422-ticket）',
    traceId: 'trace-422-ticket',
  })
})

test('resolveTicketSubmissionFailure only falls back locally for network-like failures', () => {
  const result = ticketSubmission.resolveTicketSubmissionFailure(createApiError({
    status: 503,
    message: '服务暂时不可用',
    traceId: 'trace-503-ticket',
  }))

  assert.deepEqual(result, {
    mode: 'fallback-local',
    message: '',
    traceId: '',
  })
})

test('resolveKnowledgeEditorLoadFailure keeps backend business errors visible', () => {
  const result = knowledgeEditor.resolveKnowledgeEditorLoadFailure(createApiError({
    status: 404,
    message: '文章不存在',
    traceId: 'trace-404-knowledge',
  }))

  assert.deepEqual(result, {
    mode: 'show-error',
    message: '文章不存在（追踪号：trace-404-knowledge）',
    traceId: 'trace-404-knowledge',
  })
})

test('resolveKnowledgeEditorLoadFailure falls back only for network-like failures', () => {
  const result = knowledgeEditor.resolveKnowledgeEditorLoadFailure(createApiError({
    status: 502,
    message: 'Bad Gateway',
    traceId: 'trace-502-knowledge',
  }))

  assert.deepEqual(result, {
    mode: 'fallback-local',
    message: '远程文章加载失败，当前可以继续新建本地草稿。',
    traceId: '',
  })
})

test('resolveKnowledgeEditorSaveFailure keeps business errors on the editor', () => {
  const result = knowledgeEditor.resolveKnowledgeEditorSaveFailure(createApiError({
    status: 422,
    message: '摘要长度超出限制',
    traceId: 'trace-422-knowledge',
  }), 1)

  assert.deepEqual(result, {
    mode: 'show-error',
    message: '摘要长度超出限制（追踪号：trace-422-knowledge）',
    traceId: 'trace-422-knowledge',
  })
})

test('resolveKnowledgeEditorSaveFailure falls back to local draft on 5xx errors', () => {
  const result = knowledgeEditor.resolveKnowledgeEditorSaveFailure(createApiError({
    status: 503,
    message: '服务暂时不可用',
    traceId: 'trace-503-knowledge',
  }), 0)

  assert.deepEqual(result, {
    mode: 'fallback-local',
    message: '后端不可用，草稿已保存到本地。',
    traceId: '',
  })
})

test('resolveListLoadFailure uses fallback copy for network-like failures', () => {
  const result = listLoadFailure.resolveListLoadFailure(createApiError({
    status: 503,
    message: '服务不可用',
    traceId: 'trace-503-list',
  }), {
    networkFallbackMessage: '列表接口暂时不可用',
    defaultMessage: '列表加载失败，请稍后重试。',
  })

  assert.deepEqual(result, {
    shouldUseFallbackData: true,
    message: '列表接口暂时不可用',
    traceId: '',
  })
})

test('resolveListLoadFailure preserves business error message and traceId', () => {
  const result = listLoadFailure.resolveListLoadFailure(createApiError({
    status: 403,
    message: '没有查看列表的权限',
    traceId: 'trace-403-list',
  }), {
    networkFallbackMessage: '列表接口暂时不可用',
    defaultMessage: '列表加载失败，请稍后重试。',
  })

  assert.deepEqual(result, {
    shouldUseFallbackData: true,
    message: '没有查看列表的权限（追踪号：trace-403-list）',
    traceId: 'trace-403-list',
  })
})

test('getApiErrorDisplay returns normalized message and traceId together', () => {
  const result = apiErrorDisplay.getApiErrorDisplay(createApiError({
    status: 409,
    message: '版本冲突，请刷新后重试',
    traceId: 'trace-409-generic',
  }), '操作失败，请稍后重试。')

  assert.deepEqual(result, {
    message: '版本冲突，请刷新后重试（追踪号：trace-409-generic）',
    traceId: 'trace-409-generic',
  })
})

test('resolveTicketDetailFallback suppresses network-like errors for local fallback flows', () => {
  const result = ticketDetailFailure.resolveTicketDetailFallback(createApiError({
    status: 502,
    message: 'Bad Gateway',
    traceId: 'trace-502-ticket-detail',
  }), '工单详情加载失败，请稍后重试。')

  assert.deepEqual(result, {
    mode: 'fallback',
    message: '',
    traceId: '',
  })
})

test('resolveTicketDetailFallback preserves business errors for direct display', () => {
  const result = ticketDetailFailure.resolveTicketDetailFallback(createApiError({
    status: 403,
    message: '无权访问该工单',
    traceId: 'trace-403-ticket-detail',
  }), '工单详情加载失败，请稍后重试。')

  assert.deepEqual(result, {
    mode: 'show-error',
    message: '无权访问该工单（追踪号：trace-403-ticket-detail）',
    traceId: 'trace-403-ticket-detail',
  })
})
