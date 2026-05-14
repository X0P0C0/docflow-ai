# DocFlow AI 后端错误处理规范

## 1. 目标

这份文档用来约束后端接口在异常场景下的行为，避免出现下面几类常见问题：

- 接口明明失败了，但 HTTP 状态码还是 `200`
- 不同模块各自拼错误文案，前端很难统一处理
- 日志里能看到报错，但前端拿不到 `traceId`，排查链路断掉
- 参数错误、权限错误、业务规则错误混在一起，学习和扩展都很乱

当前项目已经按这套规则完成第一轮收口。

## 2. 统一返回结构

成功或失败都走同一套响应体：

```json
{
  "code": 40001,
  "error": "VALIDATION_ERROR",
  "message": "title 不能为空",
  "data": null,
  "path": "/api/tickets",
  "timestamp": "2026-05-13T15:00:00+08:00",
  "errors": [
    {
      "field": "title",
      "message": "title 不能为空"
    }
  ],
  "traceId": "9f4f4c4f1d8a4f69"
}
```

字段说明：

- `code`: 业务错误码，给前端和文档使用
- `error`: 稳定的错误标识，适合前后端约定
- `message`: 面向用户或调用方的错误提示
- `path`: 失败请求的 URI
- `timestamp`: 服务端响应时间
- `errors`: 参数校验明细，只有校验失败时才会带
- `traceId`: 用于日志排查串联

## 3. HTTP 状态码与业务错误码分层

项目采用“两层编码”：

- HTTP 状态码：表达协议层结果，如 `400`、`401`、`403`、`404`、`409`、`422`、`500`
- `code` 字段：表达系统内部语义，如 `40001`、`40900`

这样做的好处是：

- 浏览器、网关、监控平台能正确识别失败类型
- 前端可以根据 `code` 做更细的提示和兜底
- 文档和日志可以长期稳定，不受文案变化影响

## 4. 当前标准错误码

| code | httpStatus | error | 说明 |
| --- | --- | --- | --- |
| 200 | 200 | `SUCCESS` | 请求成功 |
| 40000 | 400 | `BAD_REQUEST` | 通用请求错误 |
| 40001 | 400 | `VALIDATION_ERROR` | 参数校验失败 |
| 40100 | 401 | `AUTH_UNAUTHORIZED` | 未登录或登录过期 |
| 40300 | 403 | `AUTH_FORBIDDEN` | 无权限访问 |
| 40400 | 404 | `RESOURCE_NOT_FOUND` | 资源不存在 |
| 40900 | 409 | `RESOURCE_CONFLICT` | 状态冲突、重复操作 |
| 42200 | 422 | `BUSINESS_RULE_VIOLATION` | 不满足业务规则 |
| 50000 | 500 | `INTERNAL_SERVER_ERROR` | 系统内部异常 |

## 5. 使用约定

### 5.1 Controller 层

- 正常返回统一用 `ApiResponse.success(...)`
- 不在 Controller 手动 try/catch 后拼错误体
- 异常统一交给 `GlobalExceptionHandler`

### 5.2 Service 层

- 业务规则不满足时抛 `BusinessException`
- 能明确归类的错误，优先使用带 `ResultCode` 的构造器
- 只在确实需要时覆盖默认文案

推荐示例：

```java
throw new BusinessException(ResultCode.RESOURCE_CONFLICT, "当前工单已经指派给该处理人");
```

### 5.3 Security 层

- 未认证走 `RestAuthenticationEntryPoint`
- 已认证但无权限走 `RestAccessDeniedHandler`
- 两者都必须返回统一结构，并透出 `traceId`

## 6. 当前已落地内容

这轮已经完成：

- `GlobalExceptionHandler` 按异常类型返回正确 HTTP 状态码
- 安全异常返回统一结构
- 常见业务错误补充更准确的错误码语义
- 错误响应统一携带 `path`、`timestamp`、`traceId`
- 增加最小测试覆盖，验证 `409` / `400` / `500` 三类异常

## 7. 下一步建议

这一层打好以后，后面建议继续往下做：

1. 前端统一错误提示映射：根据 `code` 做更友好的提示文案
2. 错误码分模块整理：如 `AUTH_*`、`TICKET_*`、`KNOWLEDGE_*`
3. 接口文档同步：Swagger 中补充典型失败响应示例
4. 操作审计日志：对状态流转、指派、发布等关键动作留痕
5. 告警治理：`50000` 和高频 `4xx` 接入日志统计
