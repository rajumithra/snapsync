import io
import logging
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy import or_

from app.core.database import get_db
from app.models.models import Event, Photo, FaceEncoding
from app.schemas.schemas import FaceSearchResponse, FaceMatchItem
from app.services.face_service import face_service

logger = logging.getLogger("snapsync.face_api")

router = APIRouter(prefix="/face", tags=["Face Recognition & Matching"])

@router.post("/search", response_model=FaceSearchResponse)
async def search_photos_by_face(
    event_id_or_code: str = Form(...),
    min_similarity: float = Form(0.65),
    selfie: UploadFile = File(...),
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

    if not event.allow_face_search:
        raise HTTPException(status_code=403, detail="Face search is disabled for this event")

    # Read selfie image bytes
    selfie_bytes = await selfie.read()
    if len(selfie_bytes) == 0:
        raise HTTPException(status_code=400, detail="Empty selfie image received")

    # Detect face in selfie
    selfie_faces = face_service.detect_faces_and_embed(selfie_bytes)
    if not selfie_faces:
        raise HTTPException(
            status_code=422,
            detail="No face detected in the selfie. Please take a clear, well-lit, front-facing photo."
        )

    # Enforce strict similarity threshold (>= 0.65 for DNN SFace features) to eliminate false positives
    threshold = max(float(min_similarity), 0.65)

    # Target embedding of the guest
    guest_embedding = selfie_faces[0]["embedding"]

    # Fetch all face encodings indexed for this event
    enc_res = await db.execute(
        select(FaceEncoding, Photo)
        .join(Photo, FaceEncoding.photo_id == Photo.id)
        .where(FaceEncoding.event_id == event.id, Photo.is_hidden == False)
    )
    rows = enc_res.all()

    # Compare similarities
    matches_map = {}
    for encoding, photo in rows:
        sim = face_service.compute_similarity(guest_embedding, encoding.embedding)
        if sim >= threshold:
            # If photo already matched by another face bounding box, keep highest score
            if photo.id not in matches_map or sim > matches_map[photo.id]["similarity_score"]:
                matches_map[photo.id] = {
                    "photo_id": photo.id,
                    "similarity_score": round(sim, 3),
                    "original_filename": photo.original_filename,
                    "public_url_thumbnail": photo.public_url_thumbnail,
                    "public_url_preview": photo.public_url_preview,
                    "public_url_original": photo.public_url_original,
                    "captured_at": photo.captured_at
                }

    # Sort descending by similarity
    sorted_matches = sorted(matches_map.values(), key=lambda x: x["similarity_score"], reverse=True)

    return FaceSearchResponse(
        match_count=len(sorted_matches),
        matches=[FaceMatchItem(**m) for m in sorted_matches]
    )

@router.post("/index/{photo_id}")
async def index_photo_faces(
    photo_id: str,
    db: AsyncSession = Depends(get_db)
):
    """
    Manually triggers face re-indexing on a photo.
    """
    res = await db.execute(select(Photo).where(Photo.id == photo_id))
    photo = res.scalars().first()
    if not photo:
        raise HTTPException(status_code=404, detail="Photo not found")

    return {"message": "Face indexing initiated", "photo_id": photo_id}

