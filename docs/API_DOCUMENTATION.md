# SnapSync REST & WebSocket API Specification

Base URL: `http://localhost:8000/api/v1` (Production: `https://api.snapsync.io/api/v1`)
WebSocket URL: `ws://localhost:8000/ws/events/{event_id}`

---

## 1. Authentication (`/api/v1/auth`)

### 1.1 User Registration
- **Endpoint**: `POST /auth/register`
- **Request Body**:
```json
{
  "email": "photographer@studio.com",
  "password": "SecurePassword123!",
  "full_name": "Marcus Vance",
  "company_name": "Vance Visuals Ltd",
  "role": "photographer"
}
```
- **Response**: `201 Created`
```json
{
  "user": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "email": "photographer@studio.com",
    "full_name": "Marcus Vance",
    "role": "photographer"
  },
  "access_token": "eyJhbGciOi...",
  "token_type": "bearer"
}
```

### 1.2 User Login
- **Endpoint**: `POST /auth/login`
- **Request**: Form URL-encoded or JSON with `username` (email) and `password`.
- **Response**: `200 OK` (Returns access token and photographer profile).

### 1.3 Current User Info
- **Endpoint**: `GET /auth/me`
- **Headers**: `Authorization: Bearer <token>`

---

## 2. Event Management (`/api/v1/events`)

### 2.1 Create Event
- **Endpoint**: `POST /events`
- **Request Body**:
```json
{
  "title": "Sophia & Alexander Royal Wedding",
  "event_date": "2026-11-20",
  "location": "The Grand Estate, Napa Valley",
  "description": "Black-tie evening gala and wedding reception.",
  "is_public": true,
  "allow_face_search": true,
  "allow_guest_downloads": true
}
```
- **Response**: `201 Created`
```json
{
  "id": "e4b52b21-4d32-443b-8198-44477c7fce8a",
  "title": "Sophia & Alexander Royal Wedding",
  "event_code": "SOPHIA-ALEX-2026",
  "status": "active",
  "qr_code_url": "/api/v1/events/e4b52b21-4d32-443b-8198-44477c7fce8a/qr",
  "guest_portal_url": "https://snapsync.io/gallery/SOPHIA-ALEX-2026",
  "total_photos_count": 0,
  "created_at": "2026-11-12T19:15:00Z"
}
```

### 2.2 List Photographer's Events
- **Endpoint**: `GET /events?status=active&skip=0&limit=20`

### 2.3 Get Event Details & Public Gallery Metadata
- **Endpoint**: `GET /events/{event_id_or_code}`

### 2.4 Update Event Status
- **Endpoint**: `PATCH /events/{event_id}/status`
- **Request**: `{"status": "active"}` (`active` | `paused` | `completed`)

### 2.5 Generate / Fetch Event QR Code
- **Endpoint**: `GET /events/{event_id}/qr`
- **Response**: `image/png` or SVG QR Code image encoding guest portal URL.

---

## 3. Photo Ingestion & Cloud Upload (`/api/v1/photos`)

### 3.1 Request Pre-signed Upload URL
The mobile app calls this before streaming the photo to cloud storage.
- **Endpoint**: `POST /photos/upload-url`
- **Request Body**:
```json
{
  "event_id": "e4b52b21-4d32-443b-8198-44477c7fce8a",
  "original_filename": "DSC09241.JPG",
  "file_format": "JPEG",
  "file_size_bytes": 14258920,
  "sha256_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "camera_make": "Sony",
  "camera_model": "ILCE-7M4",
  "iso": 800,
  "shutter_speed": "1/500s",
  "aperture": "f/1.4",
  "focal_length": "85mm",
  "captured_at": "2026-11-12T19:14:22Z"
}
```
- **Response**: `200 OK`
```json
{
  "photo_id": "8f93e9a7-96a1-4328-971c-43950117d7b1",
  "upload_url_original": "https://s3.us-east-1.amazonaws.com/snapsync-photos/originals/8f93e9a7-96a1.jpg?X-Amz-Signature=...",
  "upload_url_preview": "https://s3.us-east-1.amazonaws.com/snapsync-photos/previews/8f93e9a7-96a1.webp?X-Amz-Signature=...",
  "upload_url_thumbnail": "https://s3.us-east-1.amazonaws.com/snapsync-photos/thumbs/8f93e9a7-96a1.webp?X-Amz-Signature=...",
  "storage_provider": "s3",
  "expires_in_seconds": 900
}
```

### 3.2 Direct Multipart Upload (Fallback / Local Dev)
- **Endpoint**: `POST /photos/direct-upload`
- **Multipart Form**: `file`, `preview`, `thumbnail`, `metadata` (JSON)

### 3.3 Confirm Upload Completion
- **Endpoint**: `POST /photos/confirm-upload`
- **Request Body**:
```json
{
  "photo_id": "8f93e9a7-96a1-4328-971c-43950117d7b1",
  "bytes_uploaded": 14258920,
  "status": "uploaded"
}
```
- **Action**: Triggers backend async face detection, updates DB, and broadcasts WebSocket notification.

### 3.4 Get Event Photos Gallery
- **Endpoint**: `GET /photos/event/{event_id}?skip=0&limit=50&sort=newest`
- Publicly accessible if event is public.

---

## 4. AI Face Recognition & Guest Matching (`/api/v1/face`)

### 4.1 Search Photos by Guest Selfie
Event guests scan the event QR code, click "Find My Photos", and SnapSync matches their face against the live gallery.
- **Endpoint**: `POST /face/search`
- **Multipart Form**:
  - `event_id`: UUID
  - `selfie_image`: Image file (JPEG/PNG/WebP)
  - `min_similarity`: 0.65 (default)
- **Response**: `200 OK`
```json
{
  "match_count": 8,
  "matches": [
    {
      "photo_id": "8f93e9a7-96a1-4328-971c-43950117d7b1",
      "similarity_score": 0.942,
      "thumbnail_url": "https://cdn.snapsync.io/thumbs/8f93e9a7-96a1.webp",
      "preview_url": "https://cdn.snapsync.io/previews/8f93e9a7-96a1.webp",
      "download_url": "https://cdn.snapsync.io/originals/8f93e9a7-96a1.jpg",
      "captured_at": "2026-11-12T19:14:22Z"
    }
  ]
}
```

---

## 5. Real-Time WebSocket Channel (`/ws/events/{event_id}`)

Clients (Photographer dashboard, guest live photo wall) connect to this socket to receive instantaneous updates.

### WebSocket Protocol Messages

#### 1. Incoming Event: New Photo Ingested
```json
{
  "type": "PHOTO_NEW",
  "event_id": "e4b52b21-4d32-443b-8198-44477c7fce8a",
  "data": {
    "photo_id": "8f93e9a7-96a1-4328-971c-43950117d7b1",
    "filename": "DSC09241.JPG",
    "thumbnail_url": "/static/uploads/thumbs/8f93e9a7-96a1.webp",
    "preview_url": "/static/uploads/previews/8f93e9a7-96a1.webp",
    "captured_at": "2026-11-12T19:14:22Z",
    "camera": "Sony ILCE-7M4",
    "faces_detected": 2
  }
}
```

#### 2. Incoming Event: Upload Progress
```json
{
  "type": "UPLOAD_PROGRESS",
  "event_id": "e4b52b21-4d32-443b-8198-44477c7fce8a",
  "data": {
    "photo_id": "8f93e9a7-96a1-4328-971c-43950117d7b1",
    "percent": 84,
    "speed_mbps": 12.5
  }
}
```

