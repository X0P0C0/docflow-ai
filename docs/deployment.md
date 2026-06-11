# DocFlow AI Deployment Guide

## Purpose

This document records the current local deployment baseline for `DocFlow AI`.

It reflects the active frontend migration state in this repository.

## Active Application Boundary

- Active frontend: `frontend/`
- Archived frontend snapshot: `frontend-old/`
- Archived Nuxt experiment: `frontend-v2/`
- Backend: `backend/`

All startup, debugging, and acceptance work should use `frontend/` and `backend/`.

## Runtime Requirements

- JDK 17
- Maven
- Node 22
- pnpm 11+
- MySQL 8
- Redis

## Default Ports

- Backend: `8081`
- Frontend: `8848`

## Database Initialization

Initialize the database with:

- [sql/init.sql](D:\java\project\docflow-ai\sql\init.sql)

Optional upgrade scripts:

- [sql/alter_kb_article_add_source_ticket.sql](D:\java\project\docflow-ai\sql\alter_kb_article_add_source_ticket.sql)
- [sql/alter_seed_user_passwords.sql](D:\java\project\docflow-ai\sql\alter_seed_user_passwords.sql)

Default database name:

- `docflow_ai`

## Preferred Startup

One-time setup:

```batch
scripts\setup.bat
```

Start all services:

```batch
scripts\start-all.bat
```

Stop all services:

```batch
scripts\stop-all.bat
```

## Manual Startup

### Backend

```bash
cd backend
mvn spring-boot:run
```

Health endpoints:

- `GET http://127.0.0.1:8081/api/health`
- `GET http://127.0.0.1:8081/swagger-ui.html`

### Frontend

```bash
cd frontend
pnpm install
pnpm dev
```

## Windows Encoding Notes

If PowerShell shows Chinese text as garbled, switch the console to UTF-8 before reading docs or logs:

```powershell
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
chcp 65001 > $null
```

Examples:

```powershell
Get-Content docs/deployment.md -Encoding UTF8
Invoke-WebRequest http://127.0.0.1:8081/api/health | Select-Object -ExpandProperty Content
```

## AI Claim Freshness

The backend AI workspace supports a configurable stale-claim window for shared reply drafts.

- Config key: `app.ai.claim-stale-after`
- Environment variable: `DOCFLOW_AI_CLAIM_STALE_AFTER`
- Default: `2h`

This affects:

- `GET /api/ai/workspace`
- `GET /api/ai/workspace/reply-drafts/{ticketId}`
- `POST /api/ai/workspace/reply-drafts/{ticketId}/adopt`

Accepted `Duration` examples:

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
