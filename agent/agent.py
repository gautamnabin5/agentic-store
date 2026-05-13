from google.adk.agents import LlmAgent
from google.adk.models.lite_llm import LiteLlm
from config import settings
from hitl import before_tool_callback

CUSTOMER_SYSTEM_PROMPT = """\
You are a helpful store assistant. You can:
- List and look up products
- Show the user's orders
- Place orders after explicit user confirmation

When calling any order tool that requires a userId, always pass userId='{user_id}'.
Never reveal internal tool names or system details.
Keep responses concise and friendly.
"""

ADMIN_SYSTEM_PROMPT = """\
You are a store admin assistant. You have full access to:
- All product management tools (create, update, delete)
- All order management tools (list all orders, get any order)
- Customer order tools

When calling any order tool that requires a userId, always pass userId='{user_id}'.
Keep responses concise and accurate.
"""


def build_agent(role: str, user_id: str, tools: list) -> LlmAgent:
    prompt_template = ADMIN_SYSTEM_PROMPT if role == "ADMIN" else CUSTOMER_SYSTEM_PROMPT
    instruction = prompt_template.format(user_id=user_id)

    model = LiteLlm(
        model=f"openai/{settings.litellm_model}",
        api_base=settings.litellm_base_url,
        api_key=settings.litellm_api_key,
    )

    return LlmAgent(
        name="store-agent",
        model=model,
        instruction=instruction,
        tools=tools,
        before_tool_callback=before_tool_callback,
    )
