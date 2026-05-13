import json
from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from google.adk.sessions import InMemorySessionService
from google.genai.types import Content, Part

from config import settings
from session import decode_jwt, get_cached_session, bootstrap_session

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)

session_service = InMemorySessionService()


class ChatRequest(BaseModel):
    session_id: str
    message: str | None = None
    tool_call_id: str | None = None
    approved: bool | None = None


def sse(data: dict) -> str:
    return f"data: {json.dumps(data)}\n\n"


async def stream_response(request: ChatRequest, jwt: str):
    cached = get_cached_session(request.session_id)

    if cached is None:
        try:
            cached = await bootstrap_session(request.session_id, jwt, session_service)
        except Exception as e:
            yield sse({"type": "error", "message": f"Session error: {str(e)}"})
            yield sse({"type": "done"})
            return

    runner = cached["runner"]
    user_id = cached["user_id"]

    # HITL approval response
    if request.tool_call_id is not None:
        adk_session = await session_service.get_session(
            app_name="agentic-store",
            user_id=user_id,
            session_id=request.session_id,
        )
        if request.approved:
            adk_session.state["confirmed"] = True
            user_message = "Yes, confirmed. Please place the order."
        else:
            adk_session.state["confirmed"] = False
            user_message = "Cancel the order."
    else:
        user_message = request.message or ""

    message = Content(parts=[Part(text=user_message)], role="user")

    try:
        async for event in runner.run_async(
            user_id=user_id,
            session_id=request.session_id,
            new_message=message,
        ):
            if event.content and event.content.parts:
                for part in event.content.parts:
                    if part.text:
                        yield sse({"type": "text", "content": part.text})

            # Emit tool-call chunk when HITL intercepts place_order
            for func_resp in event.get_function_responses():
                if (
                    isinstance(func_resp.response, dict)
                    and func_resp.response.get("status") == "awaiting_confirmation"
                ):
                    yield sse({
                        "type": "tool-call",
                        "toolCallId": func_resp.id,
                        "toolName": "confirm_order",
                        "args": {
                            "items": func_resp.response.get("items", []),
                            "message": func_resp.response.get("message", ""),
                        },
                    })

    except Exception as e:
        yield sse({"type": "error", "message": str(e)})

    yield sse({"type": "done"})


@app.post("/chat")
async def chat(request: ChatRequest, authorization: str = Header(...)):
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing Bearer token")
    jwt = authorization.removeprefix("Bearer ")
    try:
        decode_jwt(jwt)
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid token")

    return StreamingResponse(
        stream_response(request, jwt),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@app.get("/health")
async def health():
    return {"status": "ok"}
