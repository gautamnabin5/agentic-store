import jwt as pyjwt
from typing import Any
from config import settings

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
    return pyjwt.decode(token, settings.jwt_secret, algorithms=["HS256"])


def filter_tools_for_role(tools: list[Any], role: str) -> list[Any]:
    if role == "ADMIN":
        return tools
    return [t for t in tools if t.name not in ADMIN_ONLY_TOOLS]


def get_cached_session(session_id: str) -> dict | None:
    return _sessions.get(session_id)


def store_session(session_id: str, data: dict) -> None:
    _sessions[session_id] = data


async def bootstrap_session(session_id: str, jwt: str, session_service: Any) -> dict:
    """Bootstrap a new session: decode JWT, connect MCP, filter tools, build runner. Raises jwt.InvalidTokenError on bad token."""
    from google.adk.tools.mcp_tool.mcp_toolset import MCPToolset
    from google.adk.tools.mcp_tool.mcp_session_manager import SseServerParams
    from agent import build_agent
    from google.adk.runners import Runner

    payload = decode_jwt(jwt)
    user_id: str = payload["sub"]
    role: str = payload["role"]

    toolset = MCPToolset(
        connection_params=SseServerParams(
            url=settings.backend_mcp_url,
            headers={"Authorization": f"Bearer {jwt}"},
        )
    )
    all_tools = await toolset.get_tools_async()
    filtered = filter_tools_for_role(all_tools, role)

    agent = build_agent(role=role, user_id=user_id, tools=filtered)
    runner = Runner(agent=agent, app_name="agentic-store", session_service=session_service)

    await session_service.create_session(
        app_name="agentic-store",
        user_id=user_id,
        session_id=session_id,
        state={"user_id": user_id, "role": role, "confirmed": False},
    )

    session_data = {"runner": runner, "user_id": user_id, "role": role}
    store_session(session_id, session_data)
    return session_data
