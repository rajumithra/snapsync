# SnapSync – Live Photo Relay & Cloud Delivery Platform
## System Architecture & Technical Specification

---

## 1. System Overview

**SnapSync** is an end-to-end live photo relay and instant cloud delivery platform designed for professional event photographers, wedding studios, sports journalists, and media production agencies.

The platform closes the gap between the physical camera shutter press and instant cloud distribution:
1. **Camera-to-Phone Ingestion**: Automatically receives high-resolution JPEG and RAW photographs from Wi-Fi, hotspot, or USB-connected DSLR/Mirrorless cameras directly into an Android smartphone via an embedded, ultra-lightweight FTP server or USB PTP/MTP bridge.
2. **Edge Processing & Upload Queue**: The Android device validates the incoming file, extracts rich EXIF metadata (camera model, lens, exposure settings, shutter count), generates high-fidelity local thumbnails and web-optimized previews, and queues the asset for resilient background cloud upload.
3. **Cloud Ingestion & S3/R2 Storage**: The backend issues short-lived, pre-signed upload URLs. The Android app streams files directly to object storage (AWS S3, Cloudflare R2, or Supabase Storage), notifying the backend upon verified completion.
4. **Real-Time Distribution & WebSocket Relay**: Newly uploaded photos are immediately announced via WebSockets to connected photographer dashboards, editor suites, and public live event galleries.
5. **AI Face Recognition & Guest QR Matching**: Event attendees scan a dedicated event QR code on their smartphones, take a selfie or upload a photo, and SnapSync runs 128/512-dimensional face embedding similarity searches to filter and deliver their personalized photo gallery in real time as the event unfolds.

---

## 2. High-Level Architecture Diagram

```
+-----------------------------------------------------------------------------------+
|                              CAMERA HARDWARE LAYER                                |
|  [Sony Alpha (FTP)]   [Canon EOS (FTP/WFT)]   [Nikon Z (FTP/WT)]   [USB PTP/MTP]  |
+-----------------------------------------------------------------------------------+
                                  | Wi-Fi Hotspot / Local Network / OTG USB
                                  v
+-----------------------------------------------------------------------------------+
|                             ANDROID CLIENT ENGINE                                 |
|                                                                                   |
|  +---------------------+   +---------------------+   +-------------------------+  |
|  | Embedded FTP Server |   | USB PTP Host Engine |   | Local File System & DB  |  |
|  | (Custom NIO Socket) |   | (Android UsbManager)|   | (Encrypted Room DB)     |  |
|  +---------------------+   +---------------------+   +-------------------------+  |
|             \                         /                                           |
|              v                       v                                            |
|       +--------------------------------------------------+                        |
|       |   Image Ingestion & EXIF Metadata Extractor      |                        |
|       +--------------------------------------------------+                        |
|                                  |                                                |
|       +--------------------------------------------------+                        |
|       |   Thumbnail & Preview Generation (RenderScript)  |                        |
|       +--------------------------------------------------+                        |
|                                  |                                                |
|       +--------------------------------------------------+                        |
|       | Persistent Upload Queue (Foreground Service/Work)|                        |
|       +--------------------------------------------------+                        |
|                                  |                                                |
|       +--------------------------------------------------+                        |
|       | Jetpack Compose UI (Dashboard, Gallery, Monitor) |                        |
|       +--------------------------------------------------+                        |
+-----------------------------------------------------------------------------------+
             | REST API / Auth             | Multi-part Direct S3 Upload
             v                             v
+-----------------------------+     +-----------------------------------------------+
|     FASTAPI CLOUD BACKEND   |     |           CLOUD STORAGE & CDN                 |
|                             |     |                                               |
|  - JWT / Supabase Auth      |     |  - AWS S3 / Cloudflare R2 / Supabase Storage  |
|  - Event & Camera Org       |     |  - CloudFront / Cloudflare Edge CDN           |
|  - Presigned S3 URL Broker  |     |  - Originals Bucket (JPEG + RAW preservation) |
|  - Real-Time WebSocket Hub  |     |  - Web Previews & Thumbnails Cache            |
|  - AI Face Indexing Engine  |<--->+-----------------------------------------------+
|  - Analytics & Logs         |                            ^
+-----------------------------+                            |
        |               |                                  |
        v               v                                  |
+---------------+  +---------------------------------------+--+
|  POSTGRESQL   |  |            WEB FRONTEND CLIENTS          |
|  DATABASE     |  |                                          |
|               |  |  [Photographer & Admin Console (Next.js)]|
| - Users/Org   |  |  [Live Event Photo Wall (Real-time WS)]  |
| - Events/Cam  |  |  [Guest Portal: QR Code + Face Matcher]  |
| - Photos/Jobs |  +------------------------------------------+
| - Face Vectors|
+---------------+
```

---

## 3. Component Breakdown

### 3.1 Android Application Architecture
- **Language & Framework**: Kotlin 1.9+, Jetpack Compose, Material 3 Design System.
- **Embedded FTP Server**:
  - Implements RFC 959 (USER, PASS, TYPE, PORT, PASV, STOR, RETR, PWD, CWD, QUIT).
  - Listens on configurable port (default `2121` or `21`).
  - Active and Passive mode socket data channels.
  - Non-blocking asynchronous I/O with auto-restart on network interface change.
  - File integrity verification via checksum and stream termination detection.
- **Background Upload Engine**:
  - Foreground Service (`PhotoRelayForegroundService`) with persistent system notification showing real-time upload progress, transfer speed (MB/s), and queue length.
  - `Android WorkManager` integration for guaranteed execution across app death, OS throttling, and device restarts.
  - Network condition guards (Unmetered / Wi-Fi only mode toggle, battery-not-low constraints).
- **Local Persistence & Cache**:
  - Room SQLite Database storing `EventEntity`, `PhotoEntity`, `UploadJobEntity`, `CameraEntity`, and `SettingsEntity`.
  - Scoped storage in app-private external/internal directories (`/storage/emulated/0/Android/data/com.snapsync.app/files/photos/`).
  - Original file preservation (never downsamples or strips RAW/JPEG source files).

### 3.2 Cloud Backend Architecture
- **Framework**: Python 3.10+ with FastAPI, Uvicorn, and Pydantic v2.
- **Database Layer**: PostgreSQL 15+ with SQLAlchemy async engine and Alembic migrations.
- **Storage Subsystem**:
  - Provider-agnostic storage driver: AWS S3, Cloudflare R2, Supabase Storage, and Local Disk Driver (for development/airgapped testing).
  - Direct client-to-S3 uploads via presigned PUT URLs (zero server bandwidth bottlenecks).
- **Real-Time Communication**:
  - FastAPI WebSocket Manager (`/ws/events/{event_id}`) broadcasting:
    - `PHOTO_RECEIVED`
    - `PHOTO_UPLOADED`
    - `PHOTO_PROCESSED`
    - `EVENT_STATUS_CHANGED`
- **Face Recognition & Indexing Subsystem**:
  - Face detection & 128-dimensional embedding vector extraction (Euclidean distance & cosine similarity).
  - Real-time guest matching: guest submits a selfie -> face vector compared against indexed photos -> matching photos returned within <200ms.

### 3.3 Web Administration & Guest Portal
- **Framework**: Next.js 14 (App Router) + TypeScript + Tailwind CSS.
- **Photographer Workspace**:
  - Event setup, QR code generation, camera status monitor, upload speed telemetry, bulk photo download (ZIP), gallery management.
- **Public Guest Experience**:
  - Mobile-optimized responsive web app accessed via physical QR code scans at event tables, badges, or photo booths.
  - "Find My Photos" face-match selfie modal with instant client-side camera capture or upload.
  - Download high-res photos or share to social media.

---

## 4. End-to-End Live Relay Workflow

```
[Photographer clicks shutter on Sony A7 IV]
                      |
                      v (0.3s - 1.2s via Wi-Fi FTP)
[SnapSync Android Embedded FTP Server receives byte stream]
                      |
                      v (Atomic file flush to local storage)
[Photo Ingestion Pipeline]
  |- 1. Compute SHA-256 hash & detect duplicates
  |- 2. Parse EXIF tags (Make, Model, ISO, Shutter, Aperture, DateTaken)
  |- 3. Generate 300px WebP thumbnail & 1920px preview
  |- 4. Insert into Room DB (status: 'QUEUED')
  |- 5. Emit UI event to Transfers Screen & Dashboard
                      |
                      v
[Upload Engine]
  |- 1. Request presigned S3 upload URL from FastAPI: POST /api/v1/photos/upload-url
  |- 2. Stream original binary directly to S3/R2 via OkHttp with progress listener
  |- 3. Stream thumbnail/preview to S3/R2
  |- 4. Confirm completion: POST /api/v1/photos/confirm-upload
                      |
                      v
[FastAPI Backend Processing]
  |- 1. Verify S3 object existence and size
  |- 2. Update PostgreSQL photo record (status: 'PROCESSED')
  |- 3. Extract face embeddings in background worker
  |- 4. Broadcast WebSocket event to all connected web clients
                      |
                      v (<50ms WebSocket push)
[Live Event Web Gallery & Mobile Devices refresh instantly!]
```

---

## 5. Security & Data Protection
- **Zero Raw Credentials on Android**: Cloud storage keys (AWS Access Key/Secret) never exist on mobile devices. All uploads use cryptographically signed short-lived URLs (15-minute expiration).
- **FTP Authentication Isolation**: The Android embedded FTP server runs on a sandboxed local port with randomized credentials or user-specified PINs, isolated from public cellular routing.
- **Role-Based Access Control (RBAC)**:
  - `SUPER_ADMIN`: System-wide monitoring and storage metrics.
  - `PHOTOGRAPHER`: Restricted to owned events, camera connections, and assets.
  - `GUEST`: Read-only access to published event gallery via tokenized QR code links or face search query tokens.

