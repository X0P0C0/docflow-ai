# DocFlow AI Development Handoff

## Purpose

This document gives the next developer a reliable handoff baseline without requiring oral context.

## Current Product Shape

`DocFlow AI` is a full-stack internal operations product centered on:

- ticket workflow
- knowledge workflow
- AI-assisted workspace flow

The frontend has already migrated onto a `vue-pure-admin` shell, and the active app is now `frontend/`.

## Repository Layout

- `backend/`: Spring Boot backend
- `frontend/`: active frontend mainline
- `frontend-old/`: archived pre-migration Vite Vue snapshot
- `frontend-v2/`: archived Nuxt experiment
- `scripts/`: Windows setup and startup scripts
- `sql/`: database initialization and upgrade scripts
- `docs/`: delivery, closeout, and handoff documents

## Runtime Baseline

### Frontend

- Node: `22`
- Package manager: `pnpm`
- Working directory: `D:\java\project\docflow-ai\frontend`
- Default port: `8848`

### Backend

- JDK: `17`
- Working directory: `D:\java\project\docflow-ai\backend`
- Default port: `8081`

### Database

- Default DB: `docflow_ai`
- Init script: [sql/init.sql](D:\java\project\docflow-ai\sql\init.sql)

## Startup

### Recommended

```batch
scripts\setup.bat
scripts\start-all.bat
```

### Manual

Backend:

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
pnpm install
pnpm dev
```

## Main Product Routes

- `/login`
- `/dashboard/index`
- `/docflow/tickets/list`
- `/docflow/tickets/create`
- `/docflow/tickets/detail/:id`
- `/docflow/knowledge/list`
- `/docflow/knowledge/editor`
- `/docflow/knowledge/detail/:id`
- `/docflow/ai/index`

## Current Engineering Truth

- The active frontend is no longer the older hand-built Vite app described in legacy docs.
- The active frontend is the `vue-pure-admin` based app in `frontend/`.
- `frontend-old/` and `frontend-v2/` should not be used for routine startup, acceptance, or delivery.
- Prefer `README.md`, this file, and `docs/deployment.md` when startup guidance conflicts with older notes.

## Verification Baseline

Frontend checks currently expected on the active app:

```bash
cd frontend
pnpm typecheck
pnpm build
```

Backend full confidence still depends on Docker-backed integration runs.

## Known Remaining Cleanup

- Some older docs still need wording refresh after the frontend migration.
- Some source comments still contain encoding-garbled text and can be cleaned incrementally.
- Docker-backed acceptance is still blocked until Docker Desktop starts normally on this machine.

## Recommended Reading Order

1. [README.md](D:\java\project\docflow-ai\README.md)
2. [deployment.md](D:\java\project\docflow-ai\docs\deployment.md)
3. [frontend-delivery-demo-guide.md](D:\java\project\docflow-ai\docs\frontend-delivery-demo-guide.md)
4. [project-goal-events.md](D:\java\project\docflow-ai\docs\project-goal-events.md)
5. [project-closeout-plan.md](D:\java\project\docflow-ai\docs\project-closeout-plan.md)
