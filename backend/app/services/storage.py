import os
import uuid
from typing import Tuple, Dict, Any, Optional
from app.core.config import settings
import logging

try:
    import boto3
    from botocore.config import Config
    from botocore.exceptions import ClientError
    HAS_BOTO3 = True
except ImportError:
    HAS_BOTO3 = False

logger = logging.getLogger("snapsync.storage")

class StorageService:
    def __init__(self):
        self.provider = settings.STORAGE_PROVIDER
        os.makedirs(settings.LOCAL_STORAGE_DIR, exist_ok=True)
        os.makedirs(os.path.join(settings.LOCAL_STORAGE_DIR, "originals"), exist_ok=True)
        os.makedirs(os.path.join(settings.LOCAL_STORAGE_DIR, "previews"), exist_ok=True)
        os.makedirs(os.path.join(settings.LOCAL_STORAGE_DIR, "thumbs"), exist_ok=True)

        self.s3_client = None
        if HAS_BOTO3 and self.provider == "s3" and settings.S3_ACCESS_KEY and settings.S3_SECRET_KEY:
            try:
                self.s3_client = boto3.client(
                    "s3",
                    aws_access_key_id=settings.S3_ACCESS_KEY,
                    aws_secret_access_key=settings.S3_SECRET_KEY,
                    region_name=settings.S3_REGION,
                    endpoint_url=settings.S3_ENDPOINT_URL or None,
                    config=Config(signature_version="s3v4")
                )
            except Exception as e:
                logger.error(f"Failed to initialize S3 client: {e}. Falling back to local storage.")
                self.provider = "local"
        else:
            if self.provider == "s3" and not HAS_BOTO3:
                logger.info("boto3 not installed. Defaulting to local storage driver.")
                self.provider = "local"

    def generate_storage_keys(self, event_id: str, photo_id: str, file_ext: str) -> Dict[str, str]:
        ext = file_ext.lower().replace(".", "")
        return {
            "original": f"events/{event_id}/originals/{photo_id}.{ext}",
            "preview": f"events/{event_id}/previews/{photo_id}.webp",
            "thumbnail": f"events/{event_id}/thumbs/{photo_id}.webp"
        }

    def generate_presigned_upload_urls(
        self, event_id: str, photo_id: str, file_ext: str, expires_in: int = 900
    ) -> Dict[str, Any]:
        keys = self.generate_storage_keys(event_id, photo_id, file_ext)

        if self.provider == "s3" and self.s3_client:
            try:
                orig_url = self.s3_client.generate_presigned_url(
                    ClientMethod="put_object",
                    Params={
                        "Bucket": settings.S3_BUCKET_NAME,
                        "Key": keys["original"],
                    },
                    ExpiresIn=expires_in
                )
                prev_url = self.s3_client.generate_presigned_url(
                    ClientMethod="put_object",
                    Params={
                        "Bucket": settings.S3_BUCKET_NAME,
                        "Key": keys["preview"],
                        "ContentType": "image/webp"
                    },
                    ExpiresIn=expires_in
                )
                thumb_url = self.s3_client.generate_presigned_url(
                    ClientMethod="put_object",
                    Params={
                        "Bucket": settings.S3_BUCKET_NAME,
                        "Key": keys["thumbnail"],
                        "ContentType": "image/webp"
                    },
                    ExpiresIn=expires_in
                )
                return {
                    "provider": "s3",
                    "keys": keys,
                    "upload_url_original": orig_url,
                    "upload_url_preview": prev_url,
                    "upload_url_thumbnail": thumb_url,
                    "expires_in": expires_in
                }
            except ClientError as e:
                logger.error(f"Error creating presigned URL: {e}")

        # Local fallback endpoint
        base_url = settings.PUBLIC_CDN_URL.replace("/static/uploads", "/api/v1/photos/direct-upload")
        return {
            "provider": "local",
            "keys": keys,
            "upload_url_original": f"{base_url}?photo_id={photo_id}&target=original",
            "upload_url_preview": f"{base_url}?photo_id={photo_id}&target=preview",
            "upload_url_thumbnail": f"{base_url}?photo_id={photo_id}&target=thumbnail",
            "expires_in": expires_in
        }

    def get_public_urls(self, keys: Dict[str, str]) -> Dict[str, str]:
        if self.provider == "s3":
            base = settings.S3_ENDPOINT_URL or f"https://{settings.S3_BUCKET_NAME}.s3.{settings.S3_REGION}.amazonaws.com"
            return {
                "original": f"{base}/{keys['original']}",
                "preview": f"{base}/{keys['preview']}",
                "thumbnail": f"{base}/{keys['thumbnail']}"
            }
        else:
            return {
                "original": f"{settings.PUBLIC_CDN_URL}/{keys['original']}",
                "preview": f"{settings.PUBLIC_CDN_URL}/{keys['preview']}",
                "thumbnail": f"{settings.PUBLIC_CDN_URL}/{keys['thumbnail']}"
            }

    async def save_local_file(self, relative_path: str, content: bytes):
        target_path = os.path.join(settings.LOCAL_STORAGE_DIR, relative_path)
        os.makedirs(os.path.dirname(target_path), exist_ok=True)
        with open(target_path, "wb") as f:
            f.write(content)
        return target_path

storage_service = StorageService()
