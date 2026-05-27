# Frontend Mainline

This directory is the active frontend mainline for `DocFlow AI`.

It is based on `vue-pure-admin`, but it should now be treated as the project frontend rather than as an untouched upstream template.

## Runtime Baseline

- Node: `22`
- Package manager: `pnpm`
- Default port: `8848`

## Commands

Install dependencies:

```bash
pnpm install
```

Start development server:

```bash
pnpm dev
```

Run type check:

```bash
pnpm typecheck
```

Build production bundle:

```bash
pnpm build
```

## Boundary

- Active app: this directory
- Archived old frontend: `..\frontend-old`
- Archived Nuxt experiment: `..\frontend-v2`

Use this directory for startup, debugging, UI work, and acceptance.
