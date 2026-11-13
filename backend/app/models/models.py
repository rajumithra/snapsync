import uuid
from datetime import datetime, date
from sqlalchemy import (
    Column, String, Boolean, Integer, BigInteger, Float, 
    ForeignKey, DateTime, Date, Text, Numeric, JSON
)
from sqlalchemy.orm import relationship
from app.core.database import Base

def generate_uuid():
    return str(uuid.uuid4())

class User(Base):
    __tablename__ = "users"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    email = Column(String(255), unique=True, nullable=False, index=True)
    hashed_password = Column(String(255), nullable=False)
    full_name = Column(String(150), nullable=False)
    role = Column(String(50), default="photographer", nullable=False)  # admin, photographer, assistant, client
    is_active = Column(Boolean, default=True, nullable=False)
    is_verified = Column(Boolean, default=False, nullable=False)
    avatar_url = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    # Relationships
    profile = relationship("PhotographerProfile", back_populates="user", uselist=False, cascade="all, delete-orphan")
    events = relationship("Event", back_populates="photographer", cascade="all, delete-orphan")
    cameras = relationship("Camera", back_populates="photographer", cascade="all, delete-orphan")
    photos = relationship("Photo", back_populates="photographer", cascade="all, delete-orphan")
    storage_usage = relationship("StorageUsage", back_populates="photographer", uselist=False, cascade="all, delete-orphan")
    subscription = relationship("Subscription", back_populates="photographer", uselist=False, cascade="all, delete-orphan")
    activity_logs = relationship("ActivityLog", back_populates="user")

class PhotographerProfile(Base):
    __tablename__ = "photographer_profiles"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False)
    company_name = Column(String(200), nullable=True)
    phone = Column(String(50), nullable=True)
    website = Column(String(255), nullable=True)
    branding_logo_url = Column(Text, nullable=True)
    watermark_url = Column(Text, nullable=True)
    default_upload_resolution = Column(String(50), default="ORIGINAL")
    auto_watermark = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    user = relationship("User", back_populates="profile")

class Event(Base):
    __tablename__ = "events"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    photographer_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    title = Column(String(200), nullable=False)
    event_code = Column(String(32), unique=True, nullable=False, index=True)
    event_date = Column(Date, nullable=False, default=date.today)
    location = Column(String(255), nullable=True)
    description = Column(Text, nullable=True)
    cover_photo_url = Column(Text, nullable=True)
    status = Column(String(50), default="active", nullable=False)  # draft, active, paused, completed, archived
    qr_code_url = Column(Text, nullable=True)
    access_pin = Column(String(20), nullable=True)
    is_public = Column(Boolean, default=True, nullable=False)
    allow_face_search = Column(Boolean, default=True, nullable=False)
    allow_guest_downloads = Column(Boolean, default=True, nullable=False)
    total_photos_count = Column(Integer, default=0, nullable=False)
    total_storage_bytes = Column(BigInteger, default=0, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    photographer = relationship("User", back_populates="events")
    photos = relationship("Photo", back_populates="event", cascade="all, delete-orphan")
    camera_connections = relationship("CameraConnection", back_populates="event", cascade="all, delete-orphan")
    upload_jobs = relationship("UploadJob", back_populates="event", cascade="all, delete-orphan")
    face_encodings = relationship("FaceEncoding", back_populates="event", cascade="all, delete-orphan")

class Camera(Base):
    __tablename__ = "cameras"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    photographer_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    make = Column(String(100), nullable=False)  # Sony, Canon, Nikon, etc.
    model = Column(String(100), nullable=False)  # ILCE-7M4, EOS R5, etc.
    serial_number = Column(String(100), nullable=True)
    protocol = Column(String(50), default="ftp", nullable=False)  # ftp, usb_ptp, hotspot_ftp
    default_ftp_username = Column(String(100), default="snapsync")
    default_ftp_port = Column(Integer, default=2121)
    last_connected_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    photographer = relationship("User", back_populates="cameras")
    connections = relationship("CameraConnection", back_populates="camera", cascade="all, delete-orphan")
    photos = relationship("Photo", back_populates="camera")

class CameraConnection(Base):
    __tablename__ = "camera_connections"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    camera_id = Column(String(36), ForeignKey("cameras.id", ondelete="CASCADE"), nullable=False)
    event_id = Column(String(36), ForeignKey("events.id", ondelete="CASCADE"), nullable=False)
    device_ip = Column(String(50), nullable=True)
    device_name = Column(String(150), nullable=True)
    status = Column(String(50), default="connected", nullable=False)  # connected, disconnected, error, idle
    connected_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    disconnected_at = Column(DateTime, nullable=True)
    total_photos_received = Column(Integer, default=0, nullable=False)
    last_ping_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    camera = relationship("Camera", back_populates="connections")
    event = relationship("Event", back_populates="camera_connections")

class Photo(Base):
    __tablename__ = "photos"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    event_id = Column(String(36), ForeignKey("events.id", ondelete="CASCADE"), nullable=False, index=True)
    photographer_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    camera_id = Column(String(36), ForeignKey("cameras.id", ondelete="SET NULL"), nullable=True)
    original_filename = Column(String(255), nullable=False)
    file_format = Column(String(20), nullable=False)  # JPEG, ARW, CR3, etc.
    file_size_bytes = Column(BigInteger, nullable=False)
    sha256_hash = Column(String(64), nullable=False, index=True)
    width = Column(Integer, nullable=True)
    height = Column(Integer, nullable=True)
    orientation = Column(Integer, default=1)

    # Storage URLs
    storage_key_original = Column(Text, nullable=False)
    storage_key_preview = Column(Text, nullable=True)
    storage_key_thumbnail = Column(Text, nullable=True)
    public_url_original = Column(Text, nullable=True)
    public_url_preview = Column(Text, nullable=True)
    public_url_thumbnail = Column(Text, nullable=True)

    # EXIF
    iso = Column(Integer, nullable=True)
    shutter_speed = Column(String(50), nullable=True)
    aperture = Column(String(50), nullable=True)
    focal_length = Column(String(50), nullable=True)
    lens_model = Column(String(200), nullable=True)
    camera_make = Column(String(100), nullable=True)
    camera_model = Column(String(100), nullable=True)
    captured_at = Column(DateTime, nullable=True)

    # Status
    status = Column(String(50), default="ready", nullable=False, index=True)  # queued, uploading, uploaded, ready, failed
    has_faces = Column(Boolean, default=False)
    faces_detected_count = Column(Integer, default=0)
    is_starred = Column(Boolean, default=False)
    is_hidden = Column(Boolean, default=False)

    created_at = Column(DateTime, default=datetime.utcnow, nullable=False, index=True)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    event = relationship("Event", back_populates="photos")
    photographer = relationship("User", back_populates="photos")
    camera = relationship("Camera", back_populates="photos")
    upload_job = relationship("UploadJob", back_populates="photo", uselist=False, cascade="all, delete-orphan")
    face_encodings = relationship("FaceEncoding", back_populates="photo", cascade="all, delete-orphan")

class UploadJob(Base):
    __tablename__ = "upload_jobs"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    photo_id = Column(String(36), ForeignKey("photos.id", ondelete="CASCADE"), unique=True, nullable=False)
    event_id = Column(String(36), ForeignKey("events.id", ondelete="CASCADE"), nullable=False)
    device_id = Column(String(100), nullable=True)
    attempt_count = Column(Integer, default=0, nullable=False)
    max_retries = Column(Integer, default=5, nullable=False)
    bytes_transferred = Column(BigInteger, default=0, nullable=False)
    total_bytes = Column(BigInteger, nullable=False)
    transfer_speed_mbps = Column(Float, default=0.0)
    status = Column(String(50), default="completed", nullable=False, index=True)  # pending, in_progress, completed, failed, paused
    error_message = Column(Text, nullable=True)
    started_at = Column(DateTime, nullable=True)
    completed_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    photo = relationship("Photo", back_populates="upload_job")
    event = relationship("Event", back_populates="upload_jobs")

class FaceEncoding(Base):
    __tablename__ = "face_encodings"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    photo_id = Column(String(36), ForeignKey("photos.id", ondelete="CASCADE"), nullable=False, index=True)
    event_id = Column(String(36), ForeignKey("events.id", ondelete="CASCADE"), nullable=False, index=True)
    box_top = Column(Float, nullable=False)
    box_right = Column(Float, nullable=False)
    box_bottom = Column(Float, nullable=False)
    box_left = Column(Float, nullable=False)
    confidence = Column(Float, nullable=False)
    embedding = Column(JSON, nullable=False)  # List of float embedding coordinates
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

    photo = relationship("Photo", back_populates="face_encodings")
    event = relationship("Event", back_populates="face_encodings")

class StorageUsage(Base):
    __tablename__ = "storage_usage"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    photographer_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False)
    total_bytes_used = Column(BigInteger, default=0, nullable=False)
    allocated_limit_bytes = Column(BigInteger, default=53687091200, nullable=False)  # 50 GB
    total_photos_count = Column(Integer, default=0, nullable=False)
    total_events_count = Column(Integer, default=0, nullable=False)
    last_calculated_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    photographer = relationship("User", back_populates="storage_usage")

class Subscription(Base):
    __tablename__ = "subscriptions"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    photographer_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False)
    tier = Column(String(50), default="pro", nullable=False)  # starter, pro, studio, enterprise
    status = Column(String(50), default="active", nullable=False)  # active, past_due, canceled, trial
    storage_limit_gb = Column(Integer, default=100, nullable=False)
    max_active_events = Column(Integer, default=10, nullable=False)
    current_period_start = Column(DateTime, default=datetime.utcnow, nullable=False)
    current_period_end = Column(DateTime, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)

    photographer = relationship("User", back_populates="subscription")

class ActivityLog(Base):
    __tablename__ = "activity_logs"

    id = Column(String(36), primary_key=True, default=generate_uuid)
    user_id = Column(String(36), ForeignKey("users.id", ondelete="SET NULL"), nullable=True, index=True)
    event_id = Column(String(36), ForeignKey("events.id", ondelete="SET NULL"), nullable=True)
    action = Column(String(100), nullable=False)
    details = Column(JSON, nullable=True)
    ip_address = Column(String(50), nullable=True)
    user_agent = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False, index=True)

    user = relationship("User", back_populates="activity_logs")

