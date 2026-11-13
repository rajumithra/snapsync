from typing import List, Dict, Any
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy import func
from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.models import (
    User, Event, Camera, CameraConnection, Photo, UploadJob, StorageUsage, ActivityLog
)
from app.schemas.schemas import DashboardStats, AdminOverview

router = APIRouter(prefix="/admin", tags=["Dashboard & Administration"])

@router.get("/dashboard-stats", response_model=DashboardStats)
async def get_photographer_dashboard_stats(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    # Find active event
    ev_res = await db.execute(
        select(Event)
        .where(Event.photographer_id == current_user.id, Event.status == "active")
        .order_by(Event.created_at.desc())
    )
    active_event = ev_res.scalars().first()

    # Total connected cameras
    cam_res = await db.execute(
        select(func.count(CameraConnection.id))
        .join(Camera, CameraConnection.camera_id == Camera.id)
        .where(Camera.photographer_id == current_user.id, CameraConnection.status == "connected")
    )
    connected_cams = cam_res.scalar() or 0

    # Photo stats for active event or all events
    base_photo_query = select(Photo).where(Photo.photographer_id == current_user.id)
    if active_event:
        base_photo_query = base_photo_query.where(Photo.event_id == active_event.id)

    total_received_res = await db.execute(select(func.count(Photo.id)).where(Photo.photographer_id == current_user.id))
    total_received = total_received_res.scalar() or 0

    total_uploaded_res = await db.execute(
        select(func.count(Photo.id)).where(Photo.photographer_id == current_user.id, Photo.status == "ready")
    )
    total_uploaded = total_uploaded_res.scalar() or 0

    pending_res = await db.execute(
        select(func.count(Photo.id)).where(Photo.photographer_id == current_user.id, Photo.status.in_(["queued", "uploading"]))
    )
    pending_uploads = pending_res.scalar() or 0

    failed_res = await db.execute(
        select(func.count(Photo.id)).where(Photo.photographer_id == current_user.id, Photo.status == "failed")
    )
    failed_uploads = failed_res.scalar() or 0

    # Storage usage
    storage_res = await db.execute(
        select(StorageUsage).where(StorageUsage.photographer_id == current_user.id)
    )
    storage = storage_res.scalars().first()

    storage_used = storage.total_bytes_used if storage else 0
    storage_limit = storage.allocated_limit_bytes if storage else 53687091200

    conn_status = "ONLINE" if connected_cams > 0 else "READY"

    return DashboardStats(
        connection_status=conn_status,
        active_event_id=active_event.id if active_event else None,
        active_event_title=active_event.title if active_event else "No Active Event",
        connected_cameras_count=connected_cams,
        total_photos_received=total_received,
        total_photos_uploaded=total_uploaded,
        pending_uploads=pending_uploads,
        failed_uploads=failed_uploads,
        upload_speed_mbps=18.4 if pending_uploads > 0 else 0.0,
        storage_used_bytes=storage_used,
        storage_limit_bytes=storage_limit
    )

@router.get("/overview", response_model=AdminOverview)
async def get_admin_overview(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    if current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Super-admin access required")

    total_users = (await db.execute(select(func.count(User.id)))).scalar() or 0
    active_photographers = (await db.execute(select(func.count(User.id)).where(User.role == "photographer"))).scalar() or 0
    total_events = (await db.execute(select(func.count(Event.id)))).scalar() or 0
    total_photos = (await db.execute(select(func.count(Photo.id)))).scalar() or 0
    total_storage = (await db.execute(select(func.sum(StorageUsage.total_bytes_used)))).scalar() or 0

    logs_res = await db.execute(select(ActivityLog).order_by(ActivityLog.created_at.desc()).limit(15))
    logs = logs_res.scalars().all()

    recent_activity = [
        {"id": l.id, "action": l.action, "timestamp": l.created_at.isoformat(), "details": l.details}
        for l in logs
    ]

    return AdminOverview(
        total_users=total_users,
        active_photographers=active_photographers,
        total_events=total_events,
        total_photos=total_photos,
        total_storage_bytes=total_storage,
        recent_activity=recent_activity
    )

