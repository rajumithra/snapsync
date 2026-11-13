# SnapSync Database Schema Specification

## 1. Overview
The SnapSync database is built on PostgreSQL with UUID primary keys, relational integrity, timestamp tracking, and index optimizations for high-throughput photo ingestion, real-time status tracking, and vector-based face search.

---

## 2. Entity-Relationship Model (ERD)

```
[ users ] 1---1 [ photographer_profiles ]
    |
    +---< [ events ] 1---* [ photos ] 1---* [ face_encodings ]
    |         |                   |
    |         +---< [ cameras ]   +---1 [ upload_jobs ]
    |                   |
    |                   +---< [ camera_connections ]
    |
    +---1 [ subscriptions ]
    +---1 [ storage_usage ]
    +---* [ activity_logs ]
```

---

## 3. Table Definitions & Schemas

### 3.1 `users`
Stores system accounts (Photographers, Admins, Clients).
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'photographer' CHECK (role IN ('admin', 'photographer', 'assistant', 'client')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_verified BOOLEAN NOT NULL DEFAULT false,
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.2 `photographer_profiles`
Extended profile for photographers and photography studios.
```sql
CREATE TABLE photographer_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(200),
    phone VARCHAR(50),
    website VARCHAR(255),
    branding_logo_url TEXT,
    watermark_url TEXT,
    default_upload_resolution VARCHAR(50) DEFAULT 'ORIGINAL',
    auto_watermark BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.3 `events`
Events created by photographers (Weddings, Conferences, Sports, Galas).
```sql
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    photographer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    event_code VARCHAR(32) UNIQUE NOT NULL,
    event_date DATE NOT NULL,
    location VARCHAR(255),
    description TEXT,
    cover_photo_url TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'active', 'paused', 'completed', 'archived')),
    qr_code_url TEXT,
    access_pin VARCHAR(20),
    is_public BOOLEAN NOT NULL DEFAULT true,
    allow_face_search BOOLEAN NOT NULL DEFAULT true,
    allow_guest_downloads BOOLEAN NOT NULL DEFAULT true,
    total_photos_count INTEGER NOT NULL DEFAULT 0,
    total_storage_bytes BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.4 `cameras`
Registered camera devices owned by photographers.
```sql
CREATE TABLE cameras (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    photographer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    make VARCHAR(100) NOT NULL,           -- e.g. Sony, Canon, Nikon
    model VARCHAR(100) NOT NULL,          -- e.g. ILCE-7M4, EOS R5, Z9
    serial_number VARCHAR(100),
    protocol VARCHAR(50) NOT NULL DEFAULT 'ftp' CHECK (protocol IN ('ftp', 'usb_ptp', 'hotspot_ftp', 'wifi_direct')),
    default_ftp_username VARCHAR(100) DEFAULT 'snapsync',
    default_ftp_port INTEGER DEFAULT 2121,
    last_connected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.5 `camera_connections`
Active connection sessions between a camera, the Android device, and an event.
```sql
CREATE TABLE camera_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    camera_id UUID NOT NULL REFERENCES cameras(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    device_ip VARCHAR(50),
    device_name VARCHAR(150),
    status VARCHAR(50) NOT NULL DEFAULT 'connected' CHECK (status IN ('connected', 'disconnected', 'error', 'idle')),
    connected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    disconnected_at TIMESTAMPTZ,
    total_photos_received INTEGER NOT NULL DEFAULT 0,
    last_ping_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.6 `photos`
Main catalog of received and uploaded photographs.
```sql
CREATE TABLE photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    photographer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    camera_id UUID REFERENCES cameras(id) ON DELETE SET NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_format VARCHAR(20) NOT NULL,      -- JPEG, CR3, ARW, NEF, etc.
    file_size_bytes BIGINT NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    width INTEGER,
    height INTEGER,
    orientation INTEGER DEFAULT 1,
    
    -- Cloud Storage URIs
    storage_key_original TEXT NOT NULL,
    storage_key_preview TEXT,
    storage_key_thumbnail TEXT,
    public_url_original TEXT,
    public_url_preview TEXT,
    public_url_thumbnail TEXT,

    -- EXIF metadata
    iso INTEGER,
    shutter_speed VARCHAR(50),
    aperture VARCHAR(50),
    focal_length VARCHAR(50),
    lens_model VARCHAR(200),
    camera_make VARCHAR(100),
    camera_model VARCHAR(100),
    captured_at TIMESTAMPTZ,

    -- Status & Workflow
    status VARCHAR(50) NOT NULL DEFAULT 'queued' CHECK (status IN ('queued', 'uploading', 'uploaded', 'processing', 'ready', 'failed')),
    has_faces BOOLEAN DEFAULT false,
    faces_detected_count INTEGER DEFAULT 0,
    is_starred BOOLEAN DEFAULT false,
    is_hidden BOOLEAN DEFAULT false,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.7 `upload_jobs`
Detailed tracking for cloud uploads, retries, and failure diagnostics.
```sql
CREATE TABLE upload_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    photo_id UUID UNIQUE NOT NULL REFERENCES photos(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    device_id VARCHAR(100),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 5,
    bytes_transferred BIGINT NOT NULL DEFAULT 0,
    total_bytes BIGINT NOT NULL,
    transfer_speed_mbps NUMERIC(8,2) DEFAULT 0.0,
    status VARCHAR(50) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'in_progress', 'completed', 'failed', 'paused')),
    error_message TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.8 `face_encodings`
Stores 128-dimensional facial embedding vectors and bounding boxes for face search.
```sql
CREATE TABLE face_encodings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    photo_id UUID NOT NULL REFERENCES photos(id) ON DELETE CASCADE,
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    box_top NUMERIC(8,4) NOT NULL,
    box_right NUMERIC(8,4) NOT NULL,
    box_bottom NUMERIC(8,4) NOT NULL,
    box_left NUMERIC(8,4) NOT NULL,
    confidence NUMERIC(5,4) NOT NULL,
    embedding JSONB NOT NULL,             -- Float array of 128 facial embeddings
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.9 `storage_usage`
Aggregated storage metrics per photographer.
```sql
CREATE TABLE storage_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    photographer_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_bytes_used BIGINT NOT NULL DEFAULT 0,
    allocated_limit_bytes BIGINT NOT NULL DEFAULT 53687091200, -- Default 50 GB
    total_photos_count INTEGER NOT NULL DEFAULT 0,
    total_events_count INTEGER NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.10 `subscriptions`
Photographer tier and billing quota.
```sql
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    photographer_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tier VARCHAR(50) NOT NULL DEFAULT 'pro' CHECK (tier IN ('starter', 'pro', 'studio', 'enterprise')),
    status VARCHAR(50) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'past_due', 'canceled', 'trial')),
    storage_limit_gb INTEGER NOT NULL DEFAULT 100,
    max_active_events INTEGER NOT NULL DEFAULT 10,
    current_period_start TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    current_period_end TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '30 days',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.11 `activity_logs`
Audit log of all critical actions.
```sql
CREATE TABLE activity_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_id UUID REFERENCES events(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    details JSONB,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 4. Key Performance Indexes
```sql
CREATE INDEX idx_events_photographer ON events(photographer_id);
CREATE INDEX idx_events_code ON events(event_code);
CREATE INDEX idx_photos_event ON photos(event_id);
CREATE INDEX idx_photos_status ON photos(status);
CREATE INDEX idx_photos_created_at ON photos(created_at DESC);
CREATE INDEX idx_photos_hash ON photos(sha256_hash);
CREATE INDEX idx_upload_jobs_status ON upload_jobs(status);
CREATE INDEX idx_face_encodings_event ON face_encodings(event_id);
CREATE INDEX idx_activity_logs_user ON activity_logs(user_id);
```

