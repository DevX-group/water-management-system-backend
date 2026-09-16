# Activity Audit MCP Server

The backend exposes three read-only Model Context Protocol tools over stateless HTTP:

- `search_activity_logs`
- `get_activity_log`
- `get_entity_activity_history`

The tools reuse `ActivityAuditQueryService`; the existing REST endpoints remain unchanged.

## Output compatibility

Automatic MCP output-schema generation is disabled for these tools because Spring AI 2.0.1 does
not currently model nullable output properties correctly. System-generated audit events legitimately
have no `actorUserId` or `actorRole`; those null properties are omitted from MCP JSON responses.
The tool inputs remain schema-described, and the existing REST response contract is unchanged.

## Security

`/mcp` requires an existing Water Bill Management System JWT belonging to a `SUPER_ADMIN` or
`SYSTEM_ADMIN`. Never make the endpoint public or store an administrator JWT in the repository.

The MCP server is disabled by default in the base configuration, enabled by default for the `dev`
profile, and can be controlled with:

```text
MCP_SERVER_ENABLED=true
```

Production should explicitly set this variable only after the remote MCP authentication and network
configuration have been reviewed.

## Local verification

1. Start PostgreSQL and the backend.
2. Log in as a permitted administrator and copy the returned JWT for temporary testing.
3. Start MCP Inspector:

   ```bash
   npx @modelcontextprotocol/inspector
   ```

4. Select Streamable HTTP, enter `http://localhost:8081/mcp`, and add the request header:

   ```text
   Authorization: Bearer <temporary-admin-jwt>
   ```

5. Confirm that exactly the three read-only tools above are listed and invoke them with bounded
   pagination.

For a Claude Code demonstration, configure the same URL and temporary bearer header. A Claude.ai
Custom Connector should use an approved OAuth-compatible flow before the Render endpoint is enabled.
