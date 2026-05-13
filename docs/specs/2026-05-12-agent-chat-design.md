# Agent Chat — Design Spec

**Date:** 2026-05-12
**Status:** Approved
**Scope:** Agentic chat interface for customers to browse products and place orders, with human-in-the-loop confirmation before any order is executed.

---

## Overview

A floating chat bubble is added to the existing React frontend. It connects to a new Python agent service that runs Google ADK, routes LLM calls through the existing LiteLLM proxy, and calls the existing Spring Boot MCP server for store operations. The Spring Boot backend and database are largely untouched — the agent is a new client alongside the existing browser frontend.

This is a retrofit: the agent is layered on top of the existing REST + MCP stack without changing business logic or service signatures.

---

## Service Topology

```
Browser (React)
  │  POST /chat  +  Bearer JWT  →  SSE response stream
  ▼
agent service          (new — Python, FastAPI, port 8000)
  │  MCP/SSE + Bearer JWT (per-session)
  ├──────────────────────────────────────────────────────▶  backend (Spring Boot, :8080)
  │                                                              │
  │  LLM calls (OpenAI-compatible)                              ▼
  └──────────────────────────────────────────────────────▶  PostgreSQL (:5432)
       LiteLLM proxy (10.0.0.32) — model alias "smart"
```

The MCP endpoint (`backend:8080/mcp/sse`) is only reachable on the internal Docker network — never exposed as a public port. The agent is the only service that connects to it.

---

## New Service — `agent/`

### Responsibilities
- Accept chat messages from the frontend over HTTP (SSE response).
- Decode the user's JWT to extract `userId` and `role`.
- Establish a per-session MCP connection to Spring Boot with the JWT in headers.
- Filter the MCP tool list by role before the LLM sees it.
- Drive the ADK ReAct loop: LLM ↔ MCP tools ↔ user.
- Intercept `place_order` calls and require explicit user confirmation before executing (HITL).
- Stream events back to the frontend in TanStack AI chunk format.

### Technology
| Concern | Choice |
|---|---|
| Framework | Google ADK (Python) |
| HTTP server | FastAPI + uvicorn |
| LLM gateway | LiteLLM proxy at `10.0.0.32`, model alias `"smart"` |
| MCP transport | SSE — `MCPToolset` with `SseServerParams` |
| Session state | `InMemorySessionService` (Phase 1) |
| HITL | ADK `before_tool_call` callback |
| SSE format | TanStack AI chunk format |

### File Layout
```
agent/
  main.py          — FastAPI app, /chat endpoint
  session.py       — bootstrap: JWT decode, MCPToolset, tool filter
  agent.py         — Agent factory, role-aware system prompt
  hitl.py          — before_tool_call callback
  config.py        — settings from env vars
  requirements.txt
  Dockerfile
```

### Environment Variables
```
LITELLM_BASE_URL=http://10.0.0.32:4000
LITELLM_API_KEY=<key>
LITELLM_MODEL=smart
BACKEND_MCP_URL=http://backend:8080/mcp/sse
JWT_SECRET=<shared with backend>
```

---

## Auth Flow

The agent is treated as an untrusted client — identical to the browser. The Java service layer does not know or care that an agent is calling it.

### Session bootstrap (first message per session)
1. Frontend sends `POST /chat` with `Authorization: Bearer <jwt>` and a `session_id` UUID.
2. Agent decodes the JWT using PyJWT and the shared `JWT_SECRET`. Extracts `userId` (from `sub` claim) and `role` (from `role` claim, either `"CUSTOMER"` or `"ADMIN"`).
3. Agent creates a `MCPToolset` with `SseServerParams(url=BACKEND_MCP_URL, headers={"Authorization": "Bearer <jwt>"})`. This opens a per-session SSE connection to Spring Boot.
4. Spring Boot's `JwtAuthFilter` validates the JWT on the SSE connection and on every subsequent `POST /mcp/messages` call, setting `SecurityContext` with `ROLE_CUSTOMER` or `ROLE_ADMIN` authority.
5. Agent calls `get_tools_async()` and filters the result in Python:
   - `CUSTOMER` sessions: `list_all_orders`, `get_any_order`, `create_product`, `update_product`, `delete_product` are removed from the visible tool list.
   - `ADMIN` sessions: all tools visible.
6. Filtered tool list and `userId` are stored in `session.state` and reused for every subsequent message in the session.

### Per-turn tool calls
- The system prompt pins `userId`: `"When calling any order tool that requires a userId, always pass userId='{user_id}'."` The LLM passes it as a literal parameter — no code hook needed.
- If the LLM attempts to call an admin-only tool (`list_all_orders`, `get_any_order`) as a CUSTOMER, Spring Boot's `@PreAuthorize("hasRole('ADMIN')")` rejects it with 403. The agent surfaces this gracefully as an error message.

---

## Human-in-the-Loop (HITL) — Place Order

Ordering is irreversible. The agent must pause, show a structured summary, and require explicit confirmation before calling `place_order`.

### Mechanism
ADK's `before_tool_call` callback intercepts every tool call before it executes. For `place_order`, it:
1. Parks the intended call in `session.state["pending_tool"]`.
2. Returns a synthetic tool result (`status: "awaiting_confirmation"`) so the LLM knows to present the confirmation to the user.
3. Emits a `tool-call` SSE chunk (`toolName: "confirm_order"`) to the frontend.

The tool has **not** executed at this point.

### Frontend confirmation card
TanStack AI's `useChat` receives the `tool-call` chunk and surfaces it in `messages`. The `ChatPanel` renders a `ConfirmationCard` component showing items, quantities, unit prices, and total. Two buttons: **Confirm Order** and **Cancel**.

### Resumption
- **Confirm**: `addToolApprovalResponse({ toolCallId, approved: true })` is called. The library sends the tool result back to the agent endpoint. Agent sets `session.state["confirmed"] = True`, re-runs the conversation turn with "Yes, confirmed." `before_tool_call` sees the flag, clears it, returns `None` (allow through). `place_order` executes via MCP.
- **Cancel**: `addToolApprovalResponse({ toolCallId, approved: false })`. Agent responds: "Order cancelled."

### Why this is a hard gate
The `before_tool_call` callback is Python code — not LLM output. Even if the LLM generates "I'll place the order now" without calling the tool, nothing executes. The only path to `place_order` running is the callback seeing `confirmed = True`, which is only set by the server-side handler receiving the approval signal.

---

## SSE Stream Format (TanStack AI chunks)

The agent service emits plain JSON over SSE. This matches `@tanstack/ai-react`'s expected format.

```
data: {"type": "text", "content": "Here are products under $30..."}

data: {"type": "tool-call", "toolCallId": "abc-123", "toolName": "confirm_order", "args": {"items": [...], "total": 24.00}}

data: {"type": "done"}

data: {"type": "error", "message": "Something went wrong"}
```

---

## Session State

`InMemorySessionService` keyed by `session_id` (UUID per browser tab/open). Lost on agent container restart — acceptable for Phase 1 demo.

| Key | Value | Set when |
|---|---|---|
| `user_id` | UUID string from JWT `sub` | Session bootstrap |
| `role` | `"CUSTOMER"` or `"ADMIN"` | Session bootstrap |
| `tools` | Filtered tool list | Session bootstrap |
| `jwt` | Raw JWT string | Session bootstrap |
| `pending_tool` | `{name, args}` of intercepted call | HITL intercept |
| `confirmed` | `True` | On approval signal (cleared by callback) |
| Conversation history | ADK message list | ADK Runner (automatic) |

---

## Frontend Changes

### New components
| File | Purpose |
|---|---|
| `components/ChatBubble.tsx` | Fixed bottom-right FAB, toggles the panel open/closed |
| `components/ChatPanel.tsx` | Slide-in panel containing message list and input |
| `components/ConfirmationCard.tsx` | Renders when `messages` contains a `tool-call` for `confirm_order` |
| `components/ChatInput.tsx` | Text input + send button, disabled while `isLoading` |
| `hooks/useChatSession.ts` | Thin wrapper around `@tanstack/ai-react` `useChat` |
| `api/chat.ts` | Session ID generation, agent endpoint config |

### Library
`@tanstack/ai-react` — provides `useChat`, `fetchServerSentEvents`, and `addToolApprovalResponse`. Handles streaming text accumulation, loading state, error state, message list, and the tool approval roundtrip.

### Chat session lifecycle
1. `session_id` is generated client-side (UUID v4) when the panel first opens. Persists in component state for the lifetime of the panel.
2. Every message POST includes the `session_id` and the existing auth JWT (from the app's auth store).
3. Closing and reopening the panel generates a new `session_id` — starting a fresh session.

---

## Java Changes

The Java stack is the server. It enforces role boundaries. It does not know about the agent.

### Modified files

**`OrderService.java`** — add `@PreAuthorize` to two methods:
```java
@McpTool(name = "list_all_orders", ...)
@PreAuthorize("hasRole('ADMIN')")   // only new line
@Transactional(readOnly = true)
public List<OrderResponse> listAll() { ... }

@McpTool(name = "get_any_order", ...)
@PreAuthorize("hasRole('ADMIN')")   // only new line
@Transactional(readOnly = true)
public Result<OrderResponse> getAny(UUID orderId) { ... }
```

**`ProductService.java`** — add `@PreAuthorize` to three methods:
```java
@PreAuthorize("hasRole('ADMIN')")  // on create(), update(), delete()
```

**`SecurityConfig.java`** — add explicit MCP route rule for clarity (already covered by `anyRequest().authenticated()` but made explicit):
```java
.requestMatchers("/mcp/**").authenticated()
```

### Unchanged
Everything else: service method signatures, business logic, `@Transactional` boundaries, REST controllers, repositories, entities, DTOs, Flyway migrations.

### Why @PreAuthorize works without changes
`SecurityConfig` already has `@EnableMethodSecurity`. `JwtAuthFilter` already sets `ROLE_CUSTOMER` / `ROLE_ADMIN` as `SimpleGrantedAuthority`. The JWT on the MCP SSE connection and on each `POST /mcp/messages` goes through the same filter — SecurityContext is set correctly for every tool call.

---

## Docker Compose Addition

```yaml
agent:
  build: ./agent
  ports:
    - "8000:8000"
  environment:
    LITELLM_BASE_URL: http://10.0.0.32:4000
    LITELLM_API_KEY: ${LITELLM_API_KEY}
    LITELLM_MODEL: smart
    BACKEND_MCP_URL: http://backend:8080/mcp/sse
    JWT_SECRET: ${JWT_SECRET}
  depends_on:
    - backend
```

The agent is not reverse-proxied through the frontend Nginx — the browser calls it directly on port 8000. CORS is configured on the FastAPI app to allow the frontend origin (`CORS_ALLOWED_ORIGINS` env var, same pattern as the backend).

Port 8000 must be externally accessible on the Docker server for the browser to reach it — the CD pipeline's firewall/compose exposure must include it.

**JWT expiry**: The existing JWT TTL is 24 hours. If a session outlives the JWT, subsequent MCP calls will fail with 401. Phase 1 does not handle mid-session token refresh — the user is expected to reload and re-authenticate. This is acceptable for demo use.

---

## Out of Scope

- Cross-session memory ("order my usual") — deferred to Phase 2 with Redis-backed session service.
- Multi-agent orchestration (CatalogAgent + OrderAgent).
- Async/background agents (order watch, restock alerts).
- RAG over product catalog.
- Admin chat interface (agent scoped to ADMIN role).
- Per-role MCP tool list filtering at the Spring AI/server level.
- WebSocket transport (SSE is sufficient for one-way streaming).
- Agent-side retry / exponential backoff on MCP failures.
