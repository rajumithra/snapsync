import os
from typing import List, Optional
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "SnapSync Live Photo Relay"
    ORGANIZATION: str = "Sync AI Labs"
    WEBSITE: str = "https://syncailabs.in"
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    PORT: int = 8000
    HOST: str = "0.0.0.0"

    # Security
    SECRET_KEY: str = "snapsync-super-secret-key-change-in-production-2026"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 30  # 30 days

    # Database
    DATABASE_URL: str = "sqlite+aiosqlite:///./snapsync.db"

    # Storage
    STORAGE_PROVIDER: str = "local"  # "local", "s3", "supabase"
    LOCAL_STORAGE_DIR: str = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../../storage_uploads"))
    PUBLIC_CDN_URL: str = "http://localhost:8000/static/uploads"

    # S3 / R2 / Supabase
    S3_BUCKET_NAME: str = "snapsync-photos"
    S3_REGION: str = "us-east-1"
    S3_ENDPOINT_URL: Optional[str] = None
    S3_ACCESS_KEY: Optional[str] = None
    S3_SECRET_KEY: Optional[str] = None

    # Face Recognition
    FACE_SIMILARITY_THRESHOLD: float = 0.60
    MAX_UPLOAD_SIZE_MB: int = 50

    # CORS
    CORS_ORIGINS: List[str] = [
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://localhost:8000",
        "*"
    ]

    class Config:
        env_file = ".env"
        extra = "allow"

settings = Settings()

