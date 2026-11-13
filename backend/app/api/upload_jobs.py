from datetime import datetime
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.models import User, UploadJob
from app.schemas.schemas import UploadJobProgress, UploadJobResponse
from app.api.websocket import ws_manager

router = APIRouter(prefix="/jobs", tags=["Upload Jobs"])

@router.post("/progress")
async def report_upload_progress(
    progress: UploadJobProgress,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(UploadJob).where(UploadJob.photo_id == progress.photo_id))
    job = result.scalars().first()
    if not job:
        raise HTTPException(status_code=404, detail="Upload job not found")

    job.bytes_transferred = progress.bytes_transferred
    job.total_bytes = progress.total_bytes
    job.transfer_speed_mbps = progress.transfer_speed_mbps
    job.status = progress.status
    job.error_message = progress.error_message
    job.updated_at = datetime.utcnow()

    await db.commit()

    # Emit real-time progress update to event WebSocket
    percent = int((progress.bytes_transferred / max(1, progress.total_bytes)) * 100)
    await ws_manager.broadcast_to_event(
        event_id=job.event_id,
        message={
            "type": "UPLOAD_PROGRESS",
            "event_id": job.event_id,
            "data": {
                "photo_id": progress.photo_id,
                "percent": percent,
                "speed_mbps": progress.transfer_speed_mbps,
                "status": progress.status
            }
        }
    )

    return {"status": "recorded"}

@router.get("/event/{event_id}", response_model=List[UploadJobResponse])
async def get_event_upload_jobs(
    event_id: str,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(UploadJob).where(UploadJob.event_id == event_id).order_by(UploadJob.created_at.desc())
    )
    return result.scalars().all()

