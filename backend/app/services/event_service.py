import io
import base64
from typing import Optional
from app.core.config import settings

try:
    import qrcode
    HAS_QRCODE = True
except ImportError:
    HAS_QRCODE = False

class EventService:
    @staticmethod
    def generate_event_qr_bytes(event_code: str, web_url: Optional[str] = None) -> bytes:
        target_url = web_url or f"http://localhost:3000/gallery/{event_code}"
        
        if HAS_QRCODE:
            qr = qrcode.QRCode(
                version=1,
                error_correction=qrcode.constants.ERROR_CORRECT_M,
                box_size=10,
                border=4,
            )
            qr.add_data(target_url)
            qr.make(fit=True)

            img = qr.make_image(fill_color="#0F172A", back_color="#FFFFFF")
            buf = io.BytesIO()
            img.save(buf, format="PNG")
            return buf.getvalue()
        else:
            # Fallback 1x1 transparent/placeholder PNG bytes
            return b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15c4\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82"

    @staticmethod
    def generate_event_qr_base64(event_code: str, web_url: Optional[str] = None) -> str:
        png_bytes = EventService.generate_event_qr_bytes(event_code, web_url)
        b64 = base64.b64encode(png_bytes).decode("utf-8")
        return f"data:image/png;base64,{b64}"

event_service = EventService()
