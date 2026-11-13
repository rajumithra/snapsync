# SnapSync Production Deployment & Setup Guide

---

## 1. Prerequisites & Environment
- **Operating System**: Linux (Ubuntu 22.04 LTS recommended) or Windows 10/11 / macOS.
- **Python**: 3.10+
- **Node.js**: v18+ or v20+
- **PostgreSQL**: 14+ with pgcrypto / uuid-ossp enabled
- **Object Storage**: AWS S3, Cloudflare R2, or MinIO / Supabase Storage
- **Android SDK**: API 34+ (Android 14) and JDK 17

---

## 2. Backend Deployment

### 2.1 Virtual Environment & Dependencies
```bash
cd snapsync/backend
python -m venv venv
# Linux/macOS:
source venv/bin/activate
# Windows:
.\venv\Scripts\Activate.ps1

pip install -r requirements.txt
```

### 2.2 Environment Configuration
Copy `.env.example` to `.env` and configure:
```ini
PROJECT_NAME="SnapSync Relay Backend"
ENVIRONMENT="production"
PORT=8000
SECRET_KEY="generate-with-openssl-rand-hex-32"
DATABASE_URL="postgresql+asyncpg://postgres:password@localhost:5432/snapsync_db"

# Storage Configuration
STORAGE_PROVIDER="s3" # options: "s3", "supabase", "local"
S3_BUCKET_NAME="snapsync-cloud-photos"
S3_REGION="us-east-1"
S3_ACCESS_KEY="AKIA..."
S3_SECRET_KEY="..."
S3_CDN_DOMAIN="https://cdn.snapsync.io"

# Face Recognition Settings
FACE_SIMILARITY_THRESHOLD=0.62
ENABLE_GPU_INFERENCE=false
```

### 2.3 Database Initialization
Run PostgreSQL schema:
```bash
psql -U postgres -d snapsync_db -f migrations/schema.sql
```

### 2.4 Running Backend Service
```bash
python run.py
# Or with Gunicorn for production:
gunicorn app.main:app -w 4 -k uvicorn.workers.UvicornWorker -b 0.0.0.0:8000
```

---

## 3. Web Administration & Guest Gallery (Next.js)

### 3.1 Install & Build
```bash
cd snapsync/web
npm install
npm run build
npm start
```

### 3.2 Environment Variables (`.env.local`)
```ini
NEXT_PUBLIC_API_URL=http://localhost:8000/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8000/ws
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

---

## 4. Android Application Build & APK Generation

### 4.1 Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK Build-Tools 34.0.0

### 4.2 Building Debug APK
```bash
cd snapsync/android
# Windows:
.\gradlew.bat assembleDebug
# Linux/macOS:
./gradlew assembleDebug
```
Output APK location:
`android/app/build/outputs/apk/debug/app-debug.apk`

### 4.3 Generating Release Signed APK
```bash
# Generate release signing key if not present:
keytool -genkey -v -keystore snapsync-release.jks -alias snapsync -keyalg RSA -keysize 2048 -validity 10000

# Build release bundle / APK:
.\gradlew.bat assembleRelease
```

