from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    DATABASE_URL: str = "postgresql+asyncpg://user:password@db/shortener_db"
    REDIS_URL: str = "redis://localhost:6379"
    REDIRECT_MODE: str = "302"

settings = Settings()
