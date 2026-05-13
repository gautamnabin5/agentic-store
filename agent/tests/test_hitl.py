import pytest
from unittest.mock import MagicMock


def make_tool_context(state: dict):
    ctx = MagicMock()
    ctx.state = state
    return ctx


def make_tool(name: str):
    t = MagicMock()
    t.name = name
    return t


@pytest.mark.asyncio
async def test_place_order_without_confirmation_is_intercepted():
    from hitl import before_tool_callback

    tool = make_tool("place_order")
    args = {"userId": "abc", "items": [{"productId": "p1", "quantity": 2}]}
    state = {"confirmed": False}
    ctx = make_tool_context(state)

    result = await before_tool_callback(tool, args, ctx)

    assert result is not None
    assert result["status"] == "awaiting_confirmation"
    assert state["pending_tool"]["name"] == "place_order"
    assert state["pending_tool"]["args"] == args


@pytest.mark.asyncio
async def test_place_order_with_confirmation_is_allowed():
    from hitl import before_tool_callback

    tool = make_tool("place_order")
    args = {"userId": "abc", "items": []}
    state = {"confirmed": True}
    ctx = make_tool_context(state)

    result = await before_tool_callback(tool, args, ctx)

    assert result is None  # None = allow through
    assert state["confirmed"] is False  # flag consumed


@pytest.mark.asyncio
async def test_non_place_order_tool_is_not_intercepted():
    from hitl import before_tool_callback

    tool = make_tool("list_products")
    state = {"confirmed": False}
    ctx = make_tool_context(state)

    result = await before_tool_callback(tool, {}, ctx)
    assert result is None
