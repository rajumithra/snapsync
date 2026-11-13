# ==============================================================================
# SnapSync Backend - Production Dockerfile
# Sync AI Labs (https://syncailabs.in)
# ==============================================================================

FROM python:3.10-slim AS base

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1 \
    PORT=8000

# Install runtime and OpenCV system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Python dependencies
COPY requirements.txt .
RUN pip install --upgrade pip && \
    pip install -r requirements.txt && \
    pip install gunicorn

# Copy application source code
COPY . /app

# Ensure uploads and models directory exist
RUN mkdir -p /app/storage_uploads /app/app/ai_models

# Expose port (default 8000, dynamically overridden by $PORT on Render/Cloud)
EXPOSE 8000

# Start production server with Uvicorn worker binding dynamically to $PORT
CMD ["sh", "-c", "gunicorn app.main:app -w 2 -k uvicorn.workers.UvicornWorker -b 0.0.0.0:${PORT:-8000}"]

