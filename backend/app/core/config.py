from typing import List, Optional
from pydantic_settings import BaseSettings
from pydantic import Field


class Settings(BaseSettings):
    PROJECT_NAME: str = "DairyFlow API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    
    # Database
    DATABASE_URL: str = Field(
        default="sqlite+aiosqlite:///./dairyflow.db",
        description="Async database connection string. Supports postgresql+asyncpg:// or sqlite+aiosqlite:///"
    )
    
    # Security
    SECRET_KEY: str = Field(
        default="dairyflow-super-secret-key-change-in-production-2026-safe-key",
        description="JWT signature secret"
    )
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 12  # 12 hours
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30
    
    # Business & GST Configuration
    BUSINESS_NAME: str = "DairyFlow Co-operative & Milk Processing"
    TRADE_NAME: str = "DairyFlow Depot"
    STATE_CODE: str = "27"  # Default State Code e.g. 27 for Maharashtra
    GST_STATUS: str = "UNVERIFIED_DEMO"  # UNVERIFIED_DEMO, VERIFIED_REGISTERED, COMPOSITION, UNREGISTERED
    GSTIN: Optional[str] = None  # None until verified
    DEFAULT_DOCUMENT_TYPE: str = "COMMERCIAL_RECEIPT"  # If UNVERIFIED, commercial receipt is issued
    DEMO_WATERMARK_ENABLED: bool = True
    
    # Stock alert defaults
    DEFAULT_MIN_STOCK_ALERT: float = 20.0
    
    # CORS
    BACKEND_CORS_ORIGINS: List[str] = ["*"]

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
