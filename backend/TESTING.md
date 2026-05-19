# Backend Testing Notes

## Container Integration Tests

The backend now includes Testcontainers-based integration tests for the main AI and knowledge workflows.

Covered flows:
- MySQL and Redis container connectivity
- `POST /api/tickets/{id}/knowledge-draft`
- `POST /api/ai/workspace/reply-drafts/{ticketId}/adopt`
- `DELETE /api/ai/workspace/reply-drafts/{ticketId}/adopt`
- Service-level ticket knowledge draft creation
- Service-level AI workspace adoption persistence

## How To Run

Run the targeted container-backed tests:

```bash
mvn -q "-Dtest=InfrastructureContainerIntegrationTest,TicketKnowledgeDraftContainerIntegrationTest,AiWorkspaceContainerIntegrationTest,TicketApiContainerIntegrationTest,AiWorkspaceApiContainerIntegrationTest" test
```

Run the full backend test suite:

```bash
mvn test
```

## Docker Behavior

Container-backed tests inherit from `AbstractContainerIntegrationTest`.

- If Docker is available, the tests start real MySQL and Redis containers.
- If Docker is not available, those tests are skipped automatically because `@Testcontainers(disabledWithoutDocker = true)` is enabled on the shared base class.

This keeps local non-Docker environments usable while still allowing real dependency verification in CI or on Docker-enabled machines.
