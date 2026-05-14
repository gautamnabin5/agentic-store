import json
import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
log = logging.getLogger(__name__)

# ADK's _schema_to_dict doesn't recurse into nested schemas inside array 'items',
# so Spring AI's uppercase type strings survive into the final tool params sent to
# LiteLLM.  Patch acompletion to lowercase all 'type' values before they go out.
import google.adk.models.lite_llm as _adk_lite_llm
_orig_acompletion = _adk_lite_llm.acompletion

def _deep_lowercase_types(obj):
    if isinstance(obj, dict):
        return {k: v.lower() if k == "type" and isinstance(v, str) else _deep_lowercase_types(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [_deep_lowercase_types(item) for item in obj]
    return obj

async def _normalized_acompletion(*args, **kwargs):
    if kwargs.get("tools"):
        kwargs["tools"] = _deep_lowercase_types(kwargs["tools"])
    return await _orig_acompletion(*args, **kwargs)

_adk_lite_llm.acompletion = _normalized_acompletion

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from google.adk.sessions import InMemorySessionService
from google.genai.types import Content, Part

from config import settings
from session import decode_jwt, get_cached_session, bootstrap_session

log.info(
    "Agent starting. JWT_SECRET length=%d prefix=%r",
    len(settings.jwt_secret),
    settings.jwt_secret[:12],
)

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

session_service = InMemorySessionService()


class ChatRequest(BaseModel):
    session_id: str
    message: str | None = None
    tool_call_id: str | None = None
    approved: bool | None = None


def sse(data: dict) -> str:
    return "data: " + json.dumps(data) + chr(10) + chr(10)


async def stream_response(request: ChatRequest, jwt: str):
    log.info(
        "stream start session_id=%s has_message=%s is_hitl=%s",
        request.session_id,
        bool(request.message),
        request.tool_call_id is not None,
    )
    cached = get_cached_session(request.session_id)
    log.info("session_cache hit=%s session_id=%s", cached is not None, request.session_id)

    if cached is None:
        try:
            cached = await bootstrap_session(request.session_id, jwt, session_service)
        except Exception as e:
            log.error("bootstrap_session failed session_id=%s", request.session_id, exc_info=True)
            yield sse({"type": "error", "message": f"Session error: {str(e)}"})
            yield sse({"type": "done"})
            return

    runner = cached["runner"]
    user_id = cached["user_id"]
    log.info("using runner user_id=%s session_id=%s", user_id, request.session_id)

    if request.tool_call_id is not None:
        log.info("HITL response tool_call_id=%s approved=%s", request.tool_call_id, request.approved)
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

    log.info("sending to runner: %r", user_message[:120])
    message = Content(parts=[Part(text=user_message)], role="user")

    try:
        log.info(
            "runner.run_async start user_id=%s session_id=%s",
            user_id, request.session_id,
        )
        event_count = 0
        async for event in runner.run_async(
            user_id=user_id,
            session_id=request.session_id,
            new_message=message,
        ):
            event_count += 1
            log.info(
                "ADK event #%d author=%s has_content=%s is_final=%s",
                event_count,
                getattr(event, "author", "?"),
                bool(event.content),
                getattr(event, "is_final_response", lambda: False)(),
            )

            if event.content and event.content.parts:
                for part in event.content.parts:
                    if part.text:
                        log.info("yielding text len=%d preview=%r", len(part.text), part.text[:60])
                        yield sse({"type": "text", "content": part.text})
                    if hasattr(part, "function_call") and part.function_call:
                        log.info("function_call name=%s", part.function_call.name)
                    if hasattr(part, "function_response") and part.function_response:
                        log.info(
                            "function_response name=%s response=%r",
                            part.function_response.name,
                            str(part.function_response.response)[:120],
                        )

            for func_resp in event.get_function_responses():
                log.info(
                    "get_function_responses: id=%s response_keys=%s",
                    func_resp.id,
                    list(func_resp.response.keys()) if isinstance(func_resp.response, dict) else type(func_resp.response).__name__,
                )
                if (
                    isinstance(func_resp.response, dict)
                    and func_resp.response.get("status") == "awaiting_confirmation"
                ):
                    log.info("HITL: yielding tool-call confirm_order id=%s", func_resp.id)
                    yield sse({
                        "type": "tool-call",
                        "toolCallId": func_resp.id,
                        "toolName": "confirm_order",
                        "args": {
                            "items": func_resp.response.get("items", []),
                            "message": func_resp.response.get("message", ""),
                        },
                    })

        log.info("runner.run_async finished event_count=%d", event_count)

    except Exception as e:
        log.error("runner.run_async error session_id=%s", request.session_id, exc_info=True)
        yield sse({"type": "error", "message": str(e)})

    log.info("stream done session_id=%s", request.session_id)
    yield sse({"type": "done"})


@app.post("/chat")
async def chat(request: ChatRequest, authorization: str = Header(...)):
    log.info("POST /chat session_id=%s", request.session_id)
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing Bearer token")
    jwt = authorization.removeprefix("Bearer ")
    try:
        payload = decode_jwt(jwt)
        log.info("JWT valid user_id=%s role=%s", payload.get("sub"), payload.get("role"))
    except Exception as e:
        log.warning("Invalid JWT on POST /chat: %s (%s)", e, type(e).__name__)
        raise HTTPException(status_code=401, detail="Invalid token")

    return StreamingResponse(
        stream_response(request, jwt),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@app.get("/health")
async def health():
    return {"status": "ok"}
