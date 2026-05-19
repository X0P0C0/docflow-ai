# Integration Verification Snapshot (2026-05-19)

## Scope

This snapshot records what was re-verified on May 19, 2026 against the local backend at `http://127.0.0.1:8081`.

It is meant to answer three questions quickly:
- Which core flows are confirmed to use the real backend
- Which behaviors are expected business guardrails instead of failures
- Which areas are still frontend-only shells or fallback-oriented surfaces

## Environment

- Backend: Spring Boot service started locally on port `8081`
- Database: local MySQL reachable on port `3306`
- Redis: local Redis reachable on port `6379`
- Verified account: `admin / password`

## Verified Real Flows

The following paths were exercised successfully against the real backend:

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/tickets/{id}`
- `POST /api/tickets/{id}/comments`
- `POST /api/tickets/{id}/status`
- `GET /api/tickets/assignees`
- `POST /api/tickets/{id}/assignee`
- `GET /api/knowledge/articles`
- `POST /api/knowledge/articles`
- `PUT /api/knowledge/articles/{id}`
- `GET /api/knowledge/articles/source-ticket-counts`
- `GET /api/ai/workspace`

## Verified Cross-Module Flow

The ticket-to-knowledge path is real, but it has a business-state prerequisite.

Verified behavior:
- A ticket that is still in progress returns `422 BUSINESS_RULE_VIOLATION` from `POST /api/tickets/{id}/knowledge-draft`
- After the ticket is moved to `Resolved` (`status = 3`), `POST /api/tickets/{id}/knowledge-draft` succeeds and creates a knowledge draft linked back to the ticket

This means the flow is not blocked by missing backend capability. It is intentionally gated by business rules.

## Bug Fixed During Verification

One real backend bug was identified and fixed during this verification pass:

- `GET /api/knowledge/articles` previously returned `500 INTERNAL_SERVER_ERROR`
- Root cause: `KnowledgeArticleServiceImpl.listArticles(...)` called `sourceTicketIds.isEmpty()` even when `resolveSourceTicketIds(...)` returned `null`
- Fix: guard the empty-list short circuit with a null check

After the fix:
- `GET /api/knowledge/articles` returned successfully
- The ticket-to-knowledge verification could continue in real backend mode

## AI Center Status

`AI Center` is no longer only a static shell.

Verified behavior:
- `GET /api/ai/workspace` returns a live AI workspace payload
- `GET /api/ai/workspace/reply-drafts/{ticketId}` returns a live ticket-specific reply draft
- The payload is heuristic-based and assembled from real tickets, comments, and knowledge links
- The frontend now requests the live workspace first, can drill into ticket-specific draft detail, and falls back to a local preview only when the request fails

Current limitation:
- The new AI workspace is still a heuristic backend capability, not an external-model or generative AI integration yet

## Acceptance Checklist

Use this as the short acceptance checklist for the current state:

- Backend health responds at `GET /api/health`
- Login works with `admin / password`
- Ticket create, detail, comment, assign, and status update all work against the real backend
- Knowledge list, create, update, and source-ticket counting work against the real backend
- AI Center loads a live workspace from `GET /api/ai/workspace`
- AI Center can load a live ticket reply draft from `GET /api/ai/workspace/reply-drafts/{ticketId}`
- Ticket-to-knowledge draft creation works after the ticket reaches `Resolved` or `Closed`
- AI Center should be presented as a real heuristic backend workflow, but not yet as a full model-backed AI system

## Recommended Next Focus

The highest-value next product step is:

1. Keep the now-working real ticket and knowledge workflow stable
2. Continue shrinking fallback-only behavior where it still affects acceptance clarity
3. Deepen `AI Center` beyond heuristics into richer AI behavior once the current live workspace stabilizes
