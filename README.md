# SnapSync – Live Photo Relay & Cloud Delivery Platform

SnapSync is a professional, commercial-grade live photography relay engine built for wedding photographers, event studios, sports media, and press agencies. It automatically transfers full-resolution JPEG and RAW photographs from DSLR/Mirrorless cameras directly to an Android smartphone via an embedded FTP daemon or USB OTG, extracts EXIF metadata, generates high-speed WebP previews, streams them directly to AWS S3/Cloudflare R2 cloud storage, and provides attendees with live instant galleries featuring AI face matching.

---

## 🚀 Key Architectural Features

1. **Direct Camera-to-Phone Ingestion (Module D)**:
   - Embedded RFC 959 socket FTP daemon listening on configurable port (default `2121`).
   - Supports active/passive FTP transfers directly from Sony Alpha, Canon EOS, Nikon Z, and Lumix cameras.
   - Hotspot & venue Wi-Fi support (zero internet required between camera and phone).
   - USB OTG PTP/MTP wired tethering layer for cameras without Wi-Fi.

2. **Resilient Background Cloud Upload Engine (Module E)**:
   - Android Foreground Service with live notification progress and transfer speed telemetry.
   - Guaranteed persistent queue backed by Room SQLite and WorkManager.
   - Pre-signed S3/R2 direct upload URLs (zero server bandwidth bottlenecks).
   - SHA-256 duplicate detection and automatic retry on connection drops.

3. **AI Face Recognition & Guest QR Matching**:
   - Attendees scan event QR codes on table stands, name badges, or screens.
   - Tap **Find My Photos (Face Search)**, take a quick selfie or upload a photo.
   - SnapSync runs 128-dimensional facial embedding similarity matching against event photos and displays their personalized gallery instantly.

4. **Multi-Platform Solution**:
   - **Android**: Modern Jetpack Compose UI with dark aesthetic, Material 3, Coroutines/Flow, Room DB, Retrofit.
   - **Backend**: Python FastAPI, PostgreSQL (with SQLite zero-dependency fallback), SQLAlchemy Async, WebSockets.
   - **Web Dashboard**: Next.js 14, TypeScript, Tailwind CSS, real-time WebSocket live photo wall, Admin metrics.

---

## 📁 Repository Structure

```
snapsync/
├── docs/                           # Architecture, DB Schema & Camera Guides
│   ├── ARCHITECTURE.md             # End-to-end system design & sequence diagrams
│   ├── DATABASE_SCHEMA.md          # 10 PostgreSQL relational schemas & ERD
│   ├── CAMERA_CONFIG_GUIDE.md      # Sony, Canon, Nikon setup step-by-step
│   ├── API_DOCUMENTATION.md        # REST & WebSocket endpoint specification
│   └── DEPLOYMENT_GUIDE.md         # Production deployment & APK build manual
│
├── backend/                        # Python FastAPI Cloud Engine
│   ├── app/
│   │   ├── api/                    # Auth, Events, Photos, Face Matching, WebSocket, Admin
│   │   ├── core/                   # Config, Database engine, JWT Security
│   │   ├── models/                 # SQLAlchemy 2.0 Async ORM models
│   │   ├── schemas/                # Pydantic v2 schemas
│   │   ├── services/               # Storage (S3/R2/Local), Face Embeddings, QR codes
│   │   └── main.py                 # FastAPI application
│   ├── migrations/                 # PostgreSQL DDL migrations
│   ├── requirements.txt            # Python dependencies
│   ├── seed.py                     # Demo data seeder
│   └── run.py                      # Uvicorn entry point
│
├── web/                            # Next.js 14 Dashboard & Guest Live Gallery
│   ├── src/
│   │   ├── app/
│   │   │   ├── dashboard/          # Photographer Live Console
│   │   │   ├── events/             # Event management & QR code generator
│   │   │   ├── gallery/[eventId]/  # Public Guest Live Gallery & Face Matcher
│   │   │   └── admin/              # Super Admin system telemetry
│   │   ├── components/             # FaceMatcherModal, QRCodeModal, Navbar
│   │   └── lib/                    # API client & WebSocket listener
│   └── package.json
│
└── android/                        # Complete Android Studio Project
    ├── app/
    │   ├── src/main/java/com/snapsync/app/
    │   │   ├── service/ftp/        # Embedded RFC 959 FTP Socket Server
    │   │   ├── service/upload/     # UploadForegroundService & UploadEngine
    │   │   ├── service/usb/        # UsbCameraManager (PTP/MTP)
    │   │   ├── data/local/         # Room Database (Entities & DAOs)
    │   │   ├── data/remote/        # Retrofit API & OkHttp Streaming
    │   │   ├── util/               # EXIF Extractor, WebP Scaler, Hash & QR
    │   │   └── ui/                 # Jetpack Compose Screens & Material 3 Theme
    │   └── build.gradle.kts
    └── build.gradle.kts
```

---

## ⚡ Quick Start Guide

### 1. Launch Cloud Backend
```bash
cd backend
python -m venv venv
# Linux/macOS: source venv/bin/activate | Windows: .\venv\Scripts\Activate.ps1
pip install -r requirements.txt

# Seed sample data (Demo event: GALA-2026, Demo user: demo@snapsync.io / SnapSync2026!)
python seed.py

# Run FastAPI server on port 8000
python run.py
```
- API Docs: `http://localhost:8000/docs`
- Health check: `http://localhost:8000/health`

### 2. Launch Web Dashboard & Live Gallery
```bash
cd web
npm install
npm run dev
```
- Open `http://localhost:3000`
- Photographer Console: `http://localhost:3000/dashboard`
- Guest Live Gallery & Face Search: `http://localhost:3000/gallery/GALA-2026`
- Admin Telemetry: `http://localhost:3000/admin`

### 3. Open Android Project
1. Launch **Android Studio**.
2. Select **Open** -> Navigate to `snapsync/android`.
3. Allow Gradle to sync.
4. Run on a physical Android device (recommended for camera Wi-Fi/hotspot testing) or Android Emulator.

---

## 📸 Connecting a Camera (e.g. Sony Alpha)
1. Turn ON Android Hotspot on your phone.
2. In Sony camera: **Network** -> **FTP Transfer Func.** -> Host = `192.168.43.1`, Port = `2121`, User = `snapsync`, Password = `snapsync`, PASV = `ON`.
3. Turn **Auto FTP Transfer** ON.
4. Take a picture: the photograph immediately relays to the phone, validates EXIF, generates WebP previews, and syncs to cloud storage.

