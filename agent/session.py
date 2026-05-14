import logging
import jwt as pyjwt
from typing import Any
from config import settings

log = logging.getLogger(__name__)

ADMIN_ONLY_TOOLS = {
    "list_all_orders",
    "get_order",
    "create_product",
    "update_product",
    "delete_product",
}

# Keyed by session_id: {"runner": Runner, "user_id": str, "role": str}
_sessions: dict[str, dict] = {}


def decode_jwt(token: str) -> dict:
    log.debug(
        "decode_jwt token_len=%d prefix=%r secret_len=%d secret_prefix=%r",
        len(token), token[:20],
        len(settings.jwt_secret), settings.jwt_secret[:12],
    )
    result = pyjwt.decode(token, settings.jwt_secret, algorithms=["HS256", "HS512"])
    log.debug("decode_jwt success sub=%s role=%s", result.get("sub"), result.get("role"))
    return result


def _normalize_schema(obj: Any) -> None:
    """Recursively lowercase JSON Schema type values (Spring AI emits uppercase)."""
    if isinstance(obj, dict):
        if "type" in obj and isinstance(obj["type"], str):
            obj["type"] = obj["type"].lower()
        for v in obj.values():
            _normalize_schema(v)
    elif isinstance(obj, list):
        for item in obj:
            _normalize_schema(item)


def normalize_tool_schemas(tools: list[Any]) -> None:
    for tool in tools:
        mcp_tool = getattr(tool, "_mcp_tool", None)
        if mcp_tool is not None:
            schema = getattr(mcp_tool, "inputSchema", None)
            if isinstance(schema, dict):
                _normalize_schema(schema)
                log.debug("normalized schema for tool %s", tool.name)


def filter_tools_for_role(tools: list[Any], role: str) -> list[Any]:
    if role == "ADMIN":
        log.info("filter_tools: ADMIN gets all %d tools", len(tools))
        return tools
    filtered = [t for t in tools if t.name not in ADMIN_ONLY_TOOLS]
    log.info(
        "filter_tools: role=%s keeping %d/%d tools (removed admin-only)",
        role, len(filtered), len(tools),
    )
    return filtered


def get_cached_session(session_id: str) -> dict | None:
    cached = _sessions.get(session_id)
    log.debug("get_cached_session session_id=%s hit=%s", session_id, cached is not None)
    return cached


def store_session(session_id: str, data: dict) -> None:
    log.info("store_session session_id=%s user_id=%s role=%s", session_id, data.get("user_id"), data.get("role"))
    _sessions[session_id] = data


async def bootstrap_session(session_id: str, jwt: str, session_service: Any) -> dict:
    """Bootstrap a new session: decode JWT, connect MCP, filter tools, build runner."""
    from google.adk.tools.mcp_tool.mcp_toolset import MCPToolset
    from google.adk.tools.mcp_tool.mcp_session_manager import SseServerParams
    from agent import build_agent
    from google.adk.runners import Runner

    log.info("bootstrap_session start session_id=%s", session_id)
    payload = decode_jwt(jwt)
    user_id: str = payload["sub"]
    role: str = payload["role"]
    log.info("bootstrap_session user_id=%s role=%s", user_id, role)

    log.info("connecting MCPToolset url=%s", settings.backend_mcp_url)
    toolset = MCPToolset(
        connection_params=SseServerParams(
            url=settings.backend_mcp_url,
            headers={"Authorization": f"Bearer {jwt}"},
        )
    )
    log.info("fetching MCP tools from %s", settings.backend_mcp_url)
    try:
        all_tools = await toolset.get_tools()
        log.info("MCP tools fetched count=%d names=%s", len(all_tools), [t.name for t in all_tools])
        normalize_tool_schemas(all_tools)
        log.info("MCP tool schemas normalized")
    except Exception:
        log.error("MCPToolset.get_tools() failed", exc_info=True)
        raise

    filtered = filter_tools_for_role(all_tools, role)

    log.info("building agent role=%s", role)
    agent = build_agent(role=role, user_id=user_id, tools=filtered)
    runner = Runner(agent=agent, app_name="agentic-store", session_service=session_service)

    log.info("creating ADK session user_id=%s session_id=%s", user_id, session_id)
    await session_service.create_session(
        app_name="agentic-store",
        user_id=user_id,
        session_id=session_id,
        state={"user_id": user_id, "role": role, "confirmed": False},
    )

    log.info("bootstrap_session complete session_id=%s", session_id)
    session_data = {"runner": runner, "user_id": user_id, "role": role}
    store_session(session_id, session_data)
    return session_data
