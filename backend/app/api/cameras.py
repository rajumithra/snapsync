from datetime import datetime
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.models import User, Camera, CameraConnection, Event
from app.schemas.schemas import (
    CameraCreate, CameraResponse, CameraConnectionCreate, CameraConnectionResponse
)

router = APIRouter(prefix="/cameras", tags=["Cameras"])

@router.post("", response_model=CameraResponse, status_code=status.HTTP_201_CREATED)
async def register_camera(
    camera_in: CameraCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    camera = Camera(
        photographer_id=current_user.id,
        make=camera_in.make,
        model=camera_in.model,
        serial_number=camera_in.serial_number,
        protocol=camera_in.protocol,
        default_ftp_username=camera_in.default_ftp_username or "snapsync",
        default_ftp_port=camera_in.default_ftp_port or 2121
    )
    db.add(camera)
    await db.commit()
    await db.refresh(camera)
    return camera

@router.get("", response_model=List[CameraResponse])
async def list_cameras(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(Camera).where(Camera.photographer_id == current_user.id)
    )
    return result.scalars().all()

@router.post("/connect", response_model=CameraConnectionResponse)
async def connect_camera_to_event(
    conn_in: CameraConnectionCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    # Verify camera ownership
    cam = await db.execute(select(Camera).where(Camera.id == conn_in.camera_id, Camera.photographer_id == current_user.id))
    camera = cam.scalars().first()
    if not camera:
        raise HTTPException(status_code=404, detail="Camera not found or unauthorized")

    # Verify event ownership
    ev = await db.execute(select(Event).where(Event.id == conn_in.event_id, Event.photographer_id == current_user.id))
    event = ev.scalars().first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found or unauthorized")

    connection = CameraConnection(
        camera_id=camera.id,
        event_id=event.id,
        device_ip=conn_in.device_ip,
        device_name=conn_in.device_name,
        status="connected",
        connected_at=datetime.utcnow()
    )
    camera.last_connected_at = datetime.utcnow()
    db.add(connection)
    await db.commit()
    await db.refresh(connection)
    return connection

@router.post("/disconnect/{connection_id}")
async def disconnect_camera(
    connection_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(CameraConnection).where(CameraConnection.id == connection_id))
    conn = result.scalars().first()
    if not conn:
        raise HTTPException(status_code=404, detail="Connection not found")

    conn.status = "disconnected"
    conn.disconnected_at = datetime.utcnow()
    await db.commit()
    return {"message": "Camera disconnected successfully."}

