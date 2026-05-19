# DocFlow AI 部署说明

## 目标

这份文档用于说明当前项目的最小部署和运行前置条件。

## 运行依赖

- JDK 17
- Node 20.x
- MySQL 8
- Redis

## 后端

默认端口：

- `8081`

启动方式：

```bash
cd backend
mvn spring-boot:run
```

## 前端

默认开发端口：

- `5173`

启动方式：

```bash
cd frontend
npm install
npm run dev
```

## 数据库

初始化脚本：

- [sql/init.sql](D:\java\project\docflow-ai\sql\init.sql)

默认数据库：

- `docflow_ai`

## Windows 终端编码

如果在 Windows PowerShell 里查看文档、接口响应或日志时出现中文乱码，通常是终端输出编码问题，不是文件内容损坏。

推荐在当前会话先切到 UTF-8：

```powershell
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
chcp 65001 > $null
```

然后再读取文档或调用接口，例如：

```powershell
Get-Content docs/deployment.md -Encoding UTF8
Invoke-WebRequest http://127.0.0.1:8081/api/health | Select-Object -ExpandProperty Content
```

补充说明：

- 当前仓库里的 Markdown 文档按 UTF-8 保存
- 如果终端乱码但编辑器里中文正常，优先检查 PowerShell 编码设置
- 如需长期生效，可把上面的编码设置放进 PowerShell profile

## 当前建议

当前更适合把这份文档当作“本地与联调部署说明”。

如果后续要做更正式的部署治理，建议继续补：

- dev / test / prod 环境区分
- 环境变量清单
- 回滚与备份方案
- 一键部署或容器化方案

## AI Claim Freshness

The backend AI workspace now supports a configurable stale-claim window for shared reply drafts.

- Config key: `app.ai.claim-stale-after`
- Environment variable: `DOCFLOW_AI_CLAIM_STALE_AFTER`
- Default: `2h`

This value controls when a claimed AI reply draft is downgraded from `fresh` to `stale` in:

- `GET /api/ai/workspace`
- `GET /api/ai/workspace/reply-drafts/{ticketId}`
- `POST /api/ai/workspace/reply-drafts/{ticketId}/adopt`

Accepted Spring `Duration` formats include:

- `45m`
- `90m`
- `2h`
- `4h`

Example:

```bash
set DOCFLOW_AI_CLAIM_STALE_AFTER=45m
cd backend
mvn spring-boot:run
```
