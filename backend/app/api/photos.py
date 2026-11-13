import os
import io
import uuid
import logging
from datetime import datetime
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy import desc, or_
from PIL import Image

from app.core.database import get_db
from app.core.config import settings
from app.api.auth import get_current_user, get_optional_user
from app.models.models import User, Event, Photo, UploadJob, FaceEncoding, StorageUsage
from app.schemas.schemas import (
    PhotoUploadUrlRequest, PhotoUploadUrlResponse, PhotoConfirmUpload, PhotoResponse
)
from app.services.storage import storage_service
from app.services.face_service import face_service
from app.services.drive_service import drive_service
from app.api.websocket import ws_manager
import hashlib

logger = logging.getLogger("snapsync.photos")

router = APIRouter(prefix="/photos", tags=["Photos"])

@router.post("/upload-url", response_model=PhotoUploadUrlResponse)
async def request_upload_url(
    req: PhotoUploadUrlRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    # Verify event exists & user has permission
    ev_res = await db.execute(select(Event).where(Event.id == req.event_id))
    event = ev_res.scalars().first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if event.photographer_id != current_user.id and current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Unauthorized for this event")

    photo_id = str(uuid.uuid4())
    urls_info = storage_service.generate_presigned_upload_urls(
        event_id=event.id,
        photo_id=photo_id,
        file_ext=req.file_format
    )

    public_urls = storage_service.get_public_urls(urls_info["keys"])

    # Create photo record in 'queued' status
    photo = Photo(
        id=photo_id,
        event_id=event.id,
        photographer_id=current_user.id,
        original_filename=req.original_filename,
        file_format=req.file_format.upper(),
        file_size_bytes=req.file_size_bytes,
        sha256_hash=req.sha256_hash,
        storage_key_original=urls_info["keys"]["original"],
        storage_key_preview=urls_info["keys"]["preview"],
        storage_key_thumbnail=urls_info["keys"]["thumbnail"],
        public_url_original=public_urls["original"],
        public_url_preview=public_urls["preview"],
        public_url_thumbnail=public_urls["thumbnail"],
        camera_make=req.camera_make,
        camera_model=req.camera_model,
        iso=req.iso,
        shutter_speed=req.shutter_speed,
        aperture=req.aperture,
        focal_length=req.focal_length,
        lens_model=req.lens_model,
        captured_at=req.captured_at or datetime.utcnow(),
        status="uploading"
    )
    db.add(photo)

    job = UploadJob(
        photo_id=photo.id,
        event_id=event.id,
        total_bytes=req.file_size_bytes,
        status="in_progress",
        started_at=datetime.utcnow()
    )
    db.add(job)

    await db.commit()

    return PhotoUploadUrlResponse(
        photo_id=photo_id,
        upload_url_original=urls_info["upload_url_original"],
        upload_url_preview=urls_info.get("upload_url_preview"),
        upload_url_thumbnail=urls_info.get("upload_url_thumbnail"),
        storage_provider=urls_info["provider"],
        expires_in_seconds=urls_info["expires_in"]
    )

@router.post("/direct-upload")
async def direct_multipart_upload(
    photo_id: str = Query(...),
    target: str = Query("original"),  # original, preview, thumbnail
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(Photo).where(Photo.id == photo_id))
    photo = result.scalars().first()
    if not photo:
        raise HTTPException(status_code=404, detail="Photo record not found")

    content = await file.read()
    key_map = {
        "original": photo.storage_key_original,
        "preview": photo.storage_key_preview,
        "thumbnail": photo.storage_key_thumbnail
    }
    relative_path = key_map.get(target, photo.storage_key_original)

    saved_path = await storage_service.save_local_file(relative_path, content)

    # If it is the original file, generate preview and thumbnail if missing
    if target == "original" and photo.file_format.upper() in ["JPG", "JPEG", "PNG", "WEBP"]:
        try:
            img = Image.open(io.BytesIO(content))
            photo.width, photo.height = img.size

            # Preview (1920 max)
            prev_img = img.copy()
            prev_img.thumbnail((1920, 1920))
            prev_buf = io.BytesIO()
            prev_img.save(prev_buf, format="WEBP", quality=85)
            await storage_service.save_local_file(photo.storage_key_preview, prev_buf.getvalue())

            # Thumbnail (400 max)
            thumb_img = img.copy()
            thumb_img.thumbnail((400, 400))
            thumb_buf = io.BytesIO()
            thumb_img.save(thumb_buf, format="WEBP", quality=80)
            await storage_service.save_local_file(photo.storage_key_thumbnail, thumb_buf.getvalue())

            # Detect and index faces!
            detected_faces = face_service.detect_faces_and_embed(content)
            if detected_faces:
                photo.has_faces = True
                photo.faces_detected_count = len(detected_faces)
                for f in detected_faces:
                    encoding = FaceEncoding(
                        photo_id=photo.id,
                        event_id=photo.event_id,
                        box_top=f["box_top"],
                        box_right=f["box_right"],
                        box_bottom=f["box_bottom"],
                        box_left=f["box_left"],
                        confidence=f["confidence"],
                        embedding=f["embedding"]
                    )
                    db.add(encoding)
        except Exception as e:
            logger.warning(f"Could not auto-generate previews/faces for {photo_id}: {e}")

    await db.commit()
    return {"status": "success", "photo_id": photo_id, "target": target}

@router.post("/confirm-upload", response_model=PhotoResponse)
async def confirm_upload(
    req: PhotoConfirmUpload,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(Photo).where(Photo.id == req.photo_id))
    photo = result.scalars().first()
    if not photo:
        raise HTTPException(status_code=404, detail="Photo record not found")

    photo.status = "ready"
    photo.updated_at = datetime.utcnow()

    # Update job
    job_res = await db.execute(select(UploadJob).where(UploadJob.photo_id == photo.id))
    job = job_res.scalars().first()
    if job:
        job.status = "completed"
        job.bytes_transferred = req.bytes_uploaded
        job.completed_at = datetime.utcnow()

    # Update event counts & storage
    ev_res = await db.execute(select(Event).where(Event.id == photo.event_id))
    event = ev_res.scalars().first()
    if event:
        event.total_photos_count += 1
        event.total_storage_bytes += photo.file_size_bytes

    # Update photographer storage usage
    storage_res = await db.execute(select(StorageUsage).where(StorageUsage.photographer_id == photo.photographer_id))
    storage = storage_res.scalars().first()
    if storage:
        storage.total_bytes_used += photo.file_size_bytes
        storage.total_photos_count += 1

    await db.commit()
    await db.refresh(photo)

    # Broadcast to WebSocket subscribers for this event
    await ws_manager.broadcast_to_event(
        event_id=photo.event_id,
        message={
            "type": "PHOTO_NEW",
            "event_id": photo.event_id,
            "data": {
                "photo_id": photo.id,
                "filename": photo.original_filename,
                "thumbnail_url": photo.public_url_thumbnail,
                "preview_url": photo.public_url_preview,
                "original_url": photo.public_url_original,
                "captured_at": photo.captured_at.isoformat() if photo.captured_at else None,
                "camera": f"{photo.camera_make or ''} {photo.camera_model or ''}".strip(),
                "faces_detected": photo.faces_detected_count
            }
        }
    )

    return photo

async def ingest_binary_asset(
    db: AsyncSession,
    event: Event,
    photographer_id: str,
    filename: str,
    content: bytes
) -> Photo:
    photo_id = str(uuid.uuid4())
    file_ext = os.path.splitext(filename)[1].replace(".", "").upper() or "JPG"
    sha256_hash = hashlib.sha256(content).hexdigest()

    keys = storage_service.generate_storage_keys(event.id, photo_id, file_ext)
    public_urls = storage_service.get_public_urls(keys)

    # Save original file
    await storage_service.save_local_file(keys["original"], content)

    photo = Photo(
        id=photo_id,
        event_id=event.id,
        photographer_id=photographer_id,
        original_filename=filename,
        file_format=file_ext,
        file_size_bytes=len(content),
        sha256_hash=sha256_hash,
        storage_key_original=keys["original"],
        storage_key_preview=keys["preview"],
        storage_key_thumbnail=keys["thumbnail"],
        public_url_original=public_urls["original"],
        public_url_preview=public_urls["preview"],
        public_url_thumbnail=public_urls["thumbnail"],
        status="ready",
        captured_at=datetime.utcnow()
    )

    # Generate previews, thumbnails & extract faces
    if file_ext in ["JPG", "JPEG", "PNG", "WEBP"]:
        try:
            img = Image.open(io.BytesIO(content))
            photo.width, photo.height = img.size

            # Preview (1920)
            prev_img = img.copy()
            prev_img.thumbnail((1920, 1920))
            prev_buf = io.BytesIO()
            prev_img.save(prev_buf, format="WEBP", quality=85)
            await storage_service.save_local_file(keys["preview"], prev_buf.getvalue())

            # Thumbnail (400)
            thumb_img = img.copy()
            thumb_img.thumbnail((400, 400))
            thumb_buf = io.BytesIO()
            thumb_img.save(thumb_buf, format="WEBP", quality=80)
            await storage_service.save_local_file(keys["thumbnail"], thumb_buf.getvalue())

            # Face embeddings
            faces = face_service.detect_faces_and_embed(content)
            if faces:
                photo.has_faces = True
                photo.faces_detected_count = len(faces)
                for f in faces:
                    enc = FaceEncoding(
                        photo_id=photo.id,
                        event_id=event.id,
                        box_top=f["box_top"],
                        box_right=f["box_right"],
                        box_bottom=f["box_bottom"],
                        box_left=f["box_left"],
                        confidence=f["confidence"],
                        embedding=f["embedding"]
                    )
                    db.add(enc)
        except Exception as e:
            logger.warning(f"Error processing image metadata for {filename}: {e}")

    db.add(photo)

    # Update event counts
    event.total_photos_count += 1
    event.total_storage_bytes += len(content)

    # Update storage usage
    storage_res = await db.execute(select(StorageUsage).where(StorageUsage.photographer_id == photographer_id))
    storage = storage_res.scalars().first()
    if storage:
        storage.total_bytes_used += len(content)
        storage.total_photos_count += 1

    await db.commit()
    await db.refresh(photo)

    # Broadcast to event WebSocket
    await ws_manager.broadcast_to_event(
        event_id=event.id,
        message={
            "type": "PHOTO_NEW",
            "event_id": event.id,
            "data": {
                "photo_id": photo.id,
                "filename": photo.original_filename,
                "thumbnail_url": photo.public_url_thumbnail,
                "preview_url": photo.public_url_preview,
                "original_url": photo.public_url_original,
                "captured_at": photo.captured_at.isoformat() if photo.captured_at else None,
                "camera": "Manual Upload",
                "faces_detected": photo.faces_detected_count
            }
        }
    )

    return photo

@router.post("/manual-upload", response_model=List[PhotoResponse])
async def manual_upload_photos(
    event_id_or_code: str = Form(...),
    files: List[UploadFile] = File(...),
    db: AsyncSession = Depends(get_db)
):
    """
    Direct manual photo upload from local computer or system storage.
    """
    ev_res = await db.execute(
        select(Event).where(
            or_(Event.id == event_id_or_code, Event.event_code == event_id_or_code)
        )
    )
    event = ev_res.scalars().first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")

    saved_photos = []
    for file in files:
        content = await file.read()
        if len(content) == 0:
            continue
        photo = await ingest_binary_asset(
            db=db,
            event=event,
            photographer_id=event.photographer_id,
            filename=file.filename or f"upload_{uuid.uuid4().hex[:6]}.jpg",
            content=content
        )
        saved_photos.append(photo)

    return saved_photos

@router.post("/import-drive", response_model=List[PhotoResponse])
async def import_photos_from_google_drive(
    event_id_or_code: str = Form(...),
    drive_url: str = Form(...),
    access_token: Optional[str] = Form(None),
    db: AsyncSession = Depends(get_db)
):
    """
    Import and sync photos directly from a Google Drive shareable link or file/folder ID.
    """
    ev_res = await db.execute(
        select(Event).where(
            or_(Event.id == event_id_or_code, Event.event_code == event_id_or_code)
        )
    )
    event = ev_res.scalars().first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")

    drive_info = drive_service.extract_drive_id(drive_url.strip())
    file_id = drive_info.get("id")

    if not file_id:
        raise HTTPException(status_code=400, detail="Invalid Google Drive URL. Please provide a valid file or folder link.")

    downloaded = drive_service.download_file_by_id(file_id, access_token)
    if not downloaded:
        raise HTTPException(
            status_code=422,
            detail="Unable to download file from Google Drive. Ensure the file sharing is set to 'Anyone with the link' or provide access token."
        )

    photo = await ingest_binary_asset(
        db=db,
        event=event,
        photographer_id=event.photographer_id,
        filename=downloaded["filename"],
        content=downloaded["content"]
    )

    return [photo]

@router.get("/event/{event_id_or_code}", response_model=List[PhotoResponse])
async def list_event_photos(
    event_id_or_code: str,
    skip: int = Query(0, ge=0),
    limit: int = Query(50, ge=1, le=200),
    sort: str = Query("newest"),
    db: AsyncSession = Depends(get_db)
):
    # Resolve event
    ev_res = await db.execute(
        select(Event).where(
            or_(Event.id == event_id_or_code, Event.event_code == event_id_or_code)
        )
    )
    event = ev_res.scalars().first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")

    query = select(Photo).where(Photo.event_id == event.id, Photo.is_hidden == False)
    if sort == "newest":
        query = query.order_by(desc(Photo.captured_at), desc(Photo.created_at))
    else:
        query = query.order_by(Photo.captured_at.asc(), Photo.created_at.asc())

    query = query.offset(skip).limit(limit)
    result = await db.execute(query)
    return result.scalars().all()

@router.get("/{photo_id}", response_model=PhotoResponse)
async def get_photo(photo_id: str, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(Photo).where(Photo.id == photo_id))
    photo = result.scalars().first()
    if not photo:
        raise HTTPException(status_code=404, detail="Photo not found")
    return photo

