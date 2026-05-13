PLACE_ORDER_TOOL = "place_order"


async def before_tool_callback(tool, args: dict, tool_context) -> dict | None:
    if tool.name != PLACE_ORDER_TOOL:
        return None

    state = tool_context.state

    if state.get("confirmed"):
        state["confirmed"] = False
        return None  # allow the tool to execute

    state["pending_tool"] = {"name": tool.name, "args": args}
    return {
        "status": "awaiting_confirmation",
        "message": "This will place a real order. Please confirm.",
        "items": args.get("items", []),
    }
