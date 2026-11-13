from datetime import datetime, date
from typing import Optional, List, Any, Dict
from pydantic import BaseModel, Field

# ----------------- Auth Schemas -----------------
class UserBase(BaseModel):
    email: str
    full_name: str
    role: str = "photographer"

class UserCreate(UserBase):
    password: str
    company_name: Optional[str] = None
    phone: Optional[str] = None

class UserLogin(BaseModel):
    email: str
    password: str

class UserResponse(UserBase):
    id: str
    is_active: bool
    is_verified: bool
    avatar_url: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse

# ----------------- Profile Schemas -----------------
class ProfileUpdate(BaseModel):
    company_name: Optional[str] = None
    phone: Optional[str] = None
    website: Optional[str] = None
    branding_logo_url: Optional[str] = None
    watermark_url: Optional[str] = None
    default_upload_resolution: Optional[str] = "ORIGINAL"
    auto_watermark: Optional[bool] = False

class ProfileResponse(ProfileUpdate):
    id: str
    user_id: str
    created_at: datetime

    class Config:
        from_attributes = True

# ----------------- Event Schemas -----------------
class EventBase(BaseModel):
    title: str
    event_date: date
    location: Optional[str] = None
    description: Optional[str] = None
    is_public: bool = True
    allow_face_search: bool = True
    allow_guest_downloads: bool = True
    access_pin: Optional[str] = None

class EventCreate(EventBase):
    pass

class EventUpdate(BaseModel):
    title: Optional[str] = None
    event_date: Optional[date] = None
    location: Optional[str] = None
    description: Optional[str] = None
    status: Optional[str] = None
    is_public: Optional[bool] = None
    allow_face_search: Optional[bool] = None
    allow_guest_downloads: Optional[bool] = None
    cover_photo_url: Optional[str] = None

class EventStatusUpdate(BaseModel):
    status: str  # draft, active, paused, completed, archived

class EventResponse(EventBase):
    id: str
    photographer_id: str
    event_code: str
    status: str
    cover_photo_url: Optional[str] = None
    qr_code_url: Optional[str] = None
    total_photos_count: int
    total_storage_bytes: int
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True

# ----------------- Camera Schemas -----------------
class CameraCreate(BaseModel):
    make: str
    model: str
    serial_number: Optional[str] = None
    protocol: str = "ftp"
    default_ftp_username: Optional[str] = "snapsync"
    default_ftp_port: Optional[int] = 2121

class CameraResponse(CameraCreate):
    id: str
    photographer_id: str
    last_connected_at: Optional[datetime] = None
    created_at: datetime

    class Config:
        from_attributes = True

class CameraConnectionCreate(BaseModel):
    camera_id: str
    event_id: str
    device_ip: Optional[str] = None
    device_name: Optional[str] = None

class CameraConnectionResponse(BaseModel):
    id: str
    camera_id: str
    event_id: str
    device_ip: Optional[str] = None
    device_name: Optional[str] = None
    status: str
    connected_at: datetime
    total_photos_received: int

    class Config:
        from_attributes = True

# ----------------- Photo Schemas -----------------
class PhotoUploadUrlRequest(BaseModel):
    event_id: str
    original_filename: str
    file_format: str
    file_size_bytes: int
    sha256_hash: str
    camera_make: Optional[str] = None
    camera_model: Optional[str] = None
    iso: Optional[int] = None
    shutter_speed: Optional[str] = None
    aperture: Optional[str] = None
    focal_length: Optional[str] = None
    lens_model: Optional[str] = None
    captured_at: Optional[datetime] = None

class PhotoUploadUrlResponse(BaseModel):
    photo_id: str
    upload_url_original: str
    upload_url_preview: Optional[str] = None
    upload_url_thumbnail: Optional[str] = None
    storage_provider: str
    expires_in_seconds: int = 900

class PhotoConfirmUpload(BaseModel):
    photo_id: str
    bytes_uploaded: int
    status: str = "ready"

class PhotoResponse(BaseModel):
    id: str
    event_id: str
    original_filename: str
    file_format: str
    file_size_bytes: int
    sha256_hash: str
    width: Optional[int] = None
    height: Optional[int] = None
    public_url_original: Optional[str] = None
    public_url_preview: Optional[str] = None
    public_url_thumbnail: Optional[str] = None
    iso: Optional[int] = None
    shutter_speed: Optional[str] = None
    aperture: Optional[str] = None
    focal_length: Optional[str] = None
    camera_make: Optional[str] = None
    camera_model: Optional[str] = None
    captured_at: Optional[datetime] = None
    status: str
    has_faces: bool
    faces_detected_count: int
    created_at: datetime

    class Config:
        from_attributes = True

# ----------------- Upload Job Schemas -----------------
class UploadJobProgress(BaseModel):
    photo_id: str
    bytes_transferred: int
    total_bytes: int
    transfer_speed_mbps: float
    status: str
    error_message: Optional[str] = None

class UploadJobResponse(BaseModel):
    id: str
    photo_id: str
    event_id: str
    attempt_count: int
    bytes_transferred: int
    total_bytes: int
    transfer_speed_mbps: float
    status: str
    error_message: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True

# ----------------- Face Recognition Schemas -----------------
class FaceMatchItem(BaseModel):
    photo_id: str
    similarity_score: float
    original_filename: str
    public_url_thumbnail: Optional[str] = None
    public_url_preview: Optional[str] = None
    public_url_original: Optional[str] = None
    captured_at: Optional[datetime] = None

class FaceSearchResponse(BaseModel):
    match_count: int
    matches: List[FaceMatchItem]

# ----------------- Dashboard & Admin Stats -----------------
class DashboardStats(BaseModel):
    connection_status: str
    active_event_id: Optional[str] = None
    active_event_title: Optional[str] = None
    connected_cameras_count: int
    total_photos_received: int
    total_photos_uploaded: int
    pending_uploads: int
    failed_uploads: int
    upload_speed_mbps: float
    storage_used_bytes: int
    storage_limit_bytes: int

class AdminOverview(BaseModel):
    total_users: int
    active_photographers: int
    total_events: int
    total_photos: int
    total_storage_bytes: int
    recent_activity: List[Dict[str, Any]]
