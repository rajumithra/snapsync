import os
import re
import io
import uuid
import logging
from typing import List, Dict, Any, Optional
import requests
from PIL import Image

from app.core.config import settings
from app.services.storage import storage_service
from app.services.face_service import face_service
from app.models.models import Photo, UploadJob, FaceEncoding, Event, StorageUsage
from app.api.websocket import ws_manager

logger = logging.getLogger("snapsync.drive")

class GoogleDriveService:
    @staticmethod
    def extract_drive_id(url: str) -> Dict[str, Optional[str]]:
        """
        Extracts file_id or folder_id from various Google Drive URL formats.
        """
        # File URL patterns: /file/d/<id>/view or id=<id>
        file_match = re.search(r"/file/d/([a-zA-Z0-9_-]+)", url)
        if file_match:
            return {"type": "file", "id": file_match.group(1)}

        id_param_match = re.search(r"[?&]id=([a-zA-Z0-9_-]+)", url)
        if id_param_match:
            return {"type": "file", "id": id_param_match.group(1)}

        # Folder URL patterns: /folders/<id>
        folder_match = re.search(r"/folders/([a-zA-Z0-9_-]+)", url)
        if folder_match:
            return {"type": "folder", "id": folder_match.group(1)}

        # Direct ID entered
        if re.match(r"^[a-zA-Z0-9_-]{20,}$", url.strip()):
            return {"type": "file", "id": url.strip()}

        return {"type": "unknown", "id": None}

    @staticmethod
    def download_file_by_id(file_id: str, access_token: Optional[str] = None) -> Optional[Dict[str, Any]]:
        """
        Downloads a file from Google Drive using direct export or OAuth token.
        """
        try:
            download_url = f"https://drive.google.com/uc?export=download&id={file_id}"
            headers = {}
            if access_token:
                headers["Authorization"] = f"Bearer {access_token}"

            session = requests.Session()
            response = session.get(download_url, headers=headers, stream=True, timeout=30)

            # Check for Google Drive virus scan warning page on larger files
            if "download_warning" in response.text:
                confirm_match = re.search(r"confirm=([a-zA-Z0-9_-]+)", response.text)
                if confirm_match:
                    confirm_code = confirm_match.group(1)
                    confirm_url = f"https://drive.google.com/uc?export=download&confirm={confirm_code}&id={file_id}"
                    response = session.get(confirm_url, headers=headers, stream=True, timeout=30)

            if response.status_code == 200:
                content = response.content
                content_type = response.headers.get("Content-Type", "")

                # Guess filename
                cd = response.headers.get("Content-Disposition", "")
                fn_match = re.search(r'filename="?([^";]+)"?', cd)
                filename = fn_match.group(1) if fn_match else f"drive_{file_id[:8]}.jpg"

                return {
                    "filename": filename,
                    "content": content,
                    "size": len(content),
                    "content_type": content_type
                }
        except Exception as e:
            logger.error(f"Error downloading from Google Drive ({file_id}): {e}")

        return None

drive_service = GoogleDriveService()
