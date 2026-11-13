import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.core.config import settings
from app.core.database import init_db
from app.api import auth, events, cameras, photos, upload_jobs, face_recognition, admin, websocket

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup actions
    await init_db()
    os.makedirs(settings.LOCAL_STORAGE_DIR, exist_ok=True)
    yield
    # Shutdown actions

app = FastAPI(
    title=settings.PROJECT_NAME,
    description="Live Camera-to-Cloud Photo Relay & Instant Distribution Engine",
    version="1.0.0",
    lifespan=lifespan
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Serve local uploads statically for web & preview rendering
os.makedirs(settings.LOCAL_STORAGE_DIR, exist_ok=True)
app.mount("/static/uploads", StaticFiles(directory=settings.LOCAL_STORAGE_DIR), name="uploads")

# Include API Routers
app.include_router(auth.router, prefix="/api/v1")
app.include_router(events.router, prefix="/api/v1")
app.include_router(cameras.router, prefix="/api/v1")
app.include_router(photos.router, prefix="/api/v1")
app.include_router(upload_jobs.router, prefix="/api/v1")
app.include_router(face_recognition.router, prefix="/api/v1")
app.include_router(admin.router, prefix="/api/v1")
app.include_router(websocket.router)

@app.get("/health", tags=["Health"])
async def health_check():
    return {
        "status": "healthy",
        "service": settings.PROJECT_NAME,
        "organization": settings.ORGANIZATION,
        "website": settings.WEBSITE,
        "version": "1.0.0",
        "storage_provider": settings.STORAGE_PROVIDER
    }

@app.get("/", tags=["Root"])
async def root():
    return {
        "message": "Welcome to SnapSync Live Camera-to-Cloud API by Sync AI Labs",
        "organization": settings.ORGANIZATION,
        "website": settings.WEBSITE,
        "docs_url": "/docs",
        "health_url": "/health"
    }

