from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict


ENV_PATH = Path(__file__).resolve().parent.parent / ".env"


class Settings(BaseSettings):
    litellm_base_url: str = "http://10.0.0.32:4000"
    litellm_api_key: str = "dummy"
    litellm_model: str = "smart"
    backend_mcp_url: str = "http://backend:8080/sse"
    jwt_secret: str
    cors_allowed_origins: str = "http://localhost:5173,http://localhost"

    model_config = SettingsConfigDict(env_file=str(ENV_PATH), env_file_encoding="utf-8")

    @property
    def allowed_origins(self) -> list[str]:
        return [o.strip() for o in self.cors_allowed_origins.split(",")]


settings = Settings()
