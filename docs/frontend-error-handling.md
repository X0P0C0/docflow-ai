# DocFlow AI 前端错误处理约定

## 1. 目标

这份文档用来约束前端在调用后端接口时的错误处理方式，避免出现下面这些常见问题：

- 同一个后端错误，在不同页面提示完全不一样
- 后端明确返回业务失败，但页面误判成“接口不可用”
- 真实失败和本地演示回退混在一起，用户看不清当前状态
- `traceId`、字段级校验信息这些有价值的数据没有被用起来

当前项目已经完成第一轮统一收口。

## 2. 统一错误对象

前端统一从 `frontend/src/api/http.ts` 抛出 `ApiError`，包含这些常用字段：

- `code`: 后端业务错误码
- `status`: HTTP 状态码
- `error`: 稳定错误标识
- `message`: 后端返回的主错误文案
- `details`: 字段级校验错误列表
- `path`: 出错接口路径
- `traceId`: 便于和后端日志串联排查

当前前端已经把 `traceId` 透出到统一错误文案里，关键页面还支持直接复制追踪号，格式为：

```text
请求失败文案（追踪号：9f4f4c4f1d8a4f69）
```

这样用户提单或开发联调时，可以直接把追踪号带给后端排查。

## 3. 统一工具函数

当前统一使用下面三个工具函数：

- `getApiErrorMessage(error, fallback)`
  用于把错误转换成适合展示给用户的文案；如果后端有字段级校验信息，会优先拼接 `details`，并自动附带 `traceId`

- `isNetworkFallbackCandidate(error)`
  用于判断是否允许回退到本地演示数据

- `isApiError(error)`
  用于类型守卫和后续扩展

- `getApiErrorTraceId(error)`
  用于在后续调试面板、复制按钮或更细的错误 UI 中单独读取追踪号

## 4. 回退规则

前端采用“真实失败优先提示，服务不可用才降级”的规则。

### 4.1 可以回退到本地演示数据的场景

- 网络异常，拿不到有效 HTTP 状态码
- 服务端 `5xx`
- 本地明确就是演示模式

### 4.2 不应该回退的场景

- `400` 参数错误
- `401` 未登录或登录过期
- `403` 无权限
- `404` 资源不存在
- `409` 状态冲突
- `422` 业务规则不满足

这些场景说明后端已经清楚表达了业务结果，前端应该优先把错误提示展示出来，而不是静默切换到本地数据。

## 5. 页面处理约定

### 5.1 表单页

例如登录、创建工单、编辑知识文章：

- 先显示后端返回的明确错误信息
- 只有在网络失败或服务不可用时，才允许回退到本地流程

### 5.2 详情页

例如工单详情、知识文章详情：

- 详情加载失败时显示统一错误提示
- 操作类按钮失败时，优先展示后端错误文案
- 不把所有失败都混成“服务未启动”

### 5.3 列表页

例如工单列表、知识文章列表、Dashboard：

- 如果已回退到本地数据，页面应明确说明“当前展示的是回退数据”
- 如果只是接口返回业务错误，也可以继续展示已有数据，但提示文案不能误导成“接口不可用”

## 6. 当前已落地页面

这一轮已经统一过的页面包括：

- `LoginView.vue`
- `TicketCreateView.vue`
- `KnowledgeArticleEditorView.vue`
- `DashboardView.vue`
- `TicketDetailView.vue`
- `KnowledgeArticleDetailView.vue`
- `TicketListView.vue`
- `KnowledgeArticleListView.vue`

## 7. 推荐写法

```ts
try {
  const data = await fetchSomething()
  state.value = data
} catch (error) {
  if (isNetworkFallbackCandidate(error)) {
    state.value = fallbackData
    errorMessage.value = '后端接口暂时不可用'
    usedFallbackData.value = true
    return
  }

  errorMessage.value = getApiErrorMessage(error, '数据加载失败，请稍后重试。')
}
```

## 8. 下一步建议

后面建议继续补这几项：

1. 把 `traceId` 做成可点击复制，而不只是文本展示
2. 按模块维护错误码映射表，例如 `ticket`、`knowledge`、`auth`
3. 为关键错误场景补前端单元测试或集成测试

## 9. 401 / 403 全局处理

当前项目已经补上基础全局处理：

- `401`：清空本地登录态，并跳转到登录页
- `403`：保留当前登录态，并跳回用户可访问的页面

当前约定：

- 登录接口本身不参与全局 `401/403` 重定向，避免错误账号密码时被错误打断
- 登录页会显示“登录状态已失效，请重新登录后继续”
- 被权限挡回时，Dashboard 或知识列表页会显示明确提示

这层实现的目标不是替代页面内错误处理，而是兜住最基础的身份与权限失效场景。
