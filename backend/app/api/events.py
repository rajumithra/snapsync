import uuid
import re
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status, Response
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy import or_
from app.core.database import get_db
from app.api.auth import get_current_user, get_optional_user
from app.models.models import User, Event, ActivityLog
from app.schemas.schemas import EventCreate, EventUpdate, EventStatusUpdate, EventResponse
from app.services.event_service import event_service

router = APIRouter(prefix="/events", tags=["Events"])

def slugify(text: str) -> str:
    cleaned = re.sub(r"[^\w\s-]", "", text).strip().upper()
    return re.sub(r"[-\s]+", "-", cleaned)[:20]

@router.post("", response_model=EventResponse, status_code=status.HTTP_201_CREATED)
async def create_event(
    event_in: EventCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    # Generate unique human-readable event code
    base_slug = slugify(event_in.title) or "EVENT"
    random_suffix = uuid.uuid4().hex[:5].upper()
    event_code = f"{base_slug}-{random_suffix}"

    qr_base64 = event_service.generate_event_qr_base64(event_code)

    event = Event(
        photographer_id=current_user.id,
        title=event_in.title,
        event_code=event_code,
        event_date=event_in.event_date,
        location=event_in.location,
        description=event_in.description,
        is_public=event_in.is_public,
        allow_face_search=event_in.allow_face_search,
        allow_guest_downloads=event_in.allow_guest_downloads,
        access_pin=event_in.access_pin,
        qr_code_url=qr_base64,
        status="active"
    )
    db.add(event)

    log = ActivityLog(
        user_id=current_user.id,
        action="EVENT_CREATED",
        details={"title": event.title, "event_code": event.event_code}
    )
    db.add(log)

    await db.commit()
    await db.refresh(event)
    return event

@router.get("", response_model=List[EventResponse])
async def list_events(
    status: Optional[str] = None,
    skip: int = 0,
    limit: int = 50,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    query = select(Event).where(Event.photographer_id == current_user.id)
    if status:
        query = query.where(Event.status == status)
    query = query.order_by(Event.created_at.desc()).offset(skip).limit(limit)

    result = await db.execute(query)
    return result.scalars().all()

@router.get("/{event_id_or_code}", response_model=EventResponse)
async def get_event(
    event_id_or_code: str,
    db: AsyncSession = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_user)
):
    result = await db.execute(
        select(Event).where(
            or_(Event.id == event_id_or_code, Event.event_code == event_id_or_code)
        )
    )
    event = result.scalars().first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")

    # If event is private, verify user is the owner or admin
    if not event.is_public:
        if not current_user or (current_user.id != event.photographer_id and current_user.role != "admin"):
            raise HTTPException(status_code=403, detail="Access denied. Private event requires authorization.")

    return event

@router.put("/{event_id}", response_model=EventResponse)
async def update_event(
    event_id: str,
    event_in: EventUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(Event).where(Event.id == event_id))
    event = result.scalars().first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if event.photographer_id != current_user.id and current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Not authorized to edit this event")

    for field, val in event_in.dict(exclude_unset=True).items():
        setattr(event, field, val)

    await db.commit()
    await db.refresh(event)
    return event

@router.patch("/{event_id}/status", response_model=EventResponse)
async def update_event_status(
    event_id: str,
    status_in: EventStatusUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(select(Event).where(Event.id == event_id))
    event = result.scalars().first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")
    if event.photographer_id != current_user.id and current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Not authorized")

    event.status = status_in.status
    await db.commit()
    await db.refresh(event)
    return event

@router.get("/{event_id_or_code}/qr")
async def get_event_qr_image(
    event_id_or_code: str,
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(Event).where(
            or_(Event.id == event_id_or_code, Event.event_code == event_id_or_code)
        )
    )
    event = result.scalars().first()
    if not event:
        raise HTTPException(status_code=404, detail="Event not found")

    png_bytes = event_service.generate_event_qr_bytes(event.event_code)
    return Response(content=png_bytes, media_type="image/png")

