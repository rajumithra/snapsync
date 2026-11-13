import io
import sys
import os
import asyncio
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from httpx import AsyncClient, ASGITransport
from PIL import Image
from app.main import app

async def run_all_tests():
    print("\n--- Running SnapSync Backend E2E Tests ---", flush=True)
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        # 1. Health
        res = await client.get("/health")
        assert res.status_code == 200
        print(f"✓ Health check endpoint verified: {res.json()['status']}", flush=True)

        # 2. Login
        res = await client.post(
            "/api/v1/auth/login",
            data={"username": "demo@snapsync.io", "password": "SnapSync2026!"}
        )
        assert res.status_code == 200, f"Login failed: {res.text}"
        data = res.json()
        assert "access_token" in data
        assert data["user"]["email"] == "demo@snapsync.io"
        token = data["access_token"]
        print("✓ Authentication & JWT generation verified", flush=True)
        headers = {"Authorization": f"Bearer {token}"}

        # 3. List events
        res = await client.get("/api/v1/events", headers=headers)
        assert res.status_code == 200
        events = res.json()
        assert len(events) >= 1
        event = events[0]
        event_id = event["id"]
        event_code = event["event_code"]
        print(f"✓ Event listing verified ({len(events)} events found)", flush=True)

        # 4. Get event by code
        res = await client.get(f"/api/v1/events/{event_code}")
        assert res.status_code == 200
        print(f"✓ Event retrieval verified for code: {event_code}", flush=True)

        # 5. Request upload URL
        res = await client.post(
            "/api/v1/photos/upload-url",
            headers=headers,
            json={
                "event_id": event_id,
                "original_filename": "DSC09999.JPG",
                "file_format": "JPEG",
                "file_size_bytes": 102400,
                "sha256_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "camera_make": "Sony",
                "camera_model": "ILCE-7M4",
                "iso": 800,
                "shutter_speed": "1/500s",
                "aperture": "f/1.4",
                "focal_length": "85mm"
            }
        )
        assert res.status_code == 200
        upload_info = res.json()
        photo_id = upload_info["photo_id"]
        print("✓ Photo upload-url brokered successfully", flush=True)

        # 6. Upload test image binary
        img = Image.new("RGB", (400, 400), color=(100, 150, 200))
        buf = io.BytesIO()
        img.save(buf, format="JPEG")
        img_bytes = buf.getvalue()

        upload_res = await client.post(
            f"/api/v1/photos/direct-upload?photo_id={photo_id}&target=original",
            files={"file": ("DSC09999.JPG", img_bytes, "image/jpeg")}
        )
        assert upload_res.status_code == 200
        print("✓ Photo binary uploaded & thumbnail generated", flush=True)

        # 7. Confirm upload
        confirm_res = await client.post(
            "/api/v1/photos/confirm-upload",
            json={"photo_id": photo_id, "bytes_uploaded": len(img_bytes), "status": "ready"}
        )
        assert confirm_res.status_code == 200
        print("✓ Upload confirmed & indexed", flush=True)

        # 8. Retrieve event gallery
        gallery_res = await client.get(f"/api/v1/photos/event/{event_code}")
        assert gallery_res.status_code == 200
        gallery_photos = gallery_res.json()
        assert len(gallery_photos) >= 1
        print(f"✓ Event gallery retrieved ({len(gallery_photos)} photos)", flush=True)

        # 9. AI Face Search
        selfie_img = Image.new("RGB", (300, 300), color=(220, 180, 140))
        selfie_buf = io.BytesIO()
        selfie_img.save(selfie_buf, format="JPEG")
        selfie_bytes = selfie_buf.getvalue()

        face_res = await client.post(
            "/api/v1/face/search",
            data={"event_id_or_code": event_code, "min_similarity": "0.50"},
            files={"selfie": ("selfie.jpg", selfie_bytes, "image/jpeg")}
        )
        assert face_res.status_code == 200
        search_data = face_res.json()
        assert "matches" in search_data
        print(f"✓ AI Face Search endpoint verified (Found {search_data['match_count']} matches)", flush=True)

        # 10. Dashboard stats
        stats_res = await client.get("/api/v1/admin/dashboard-stats", headers=headers)
        assert stats_res.status_code == 200
        stats = stats_res.json()
        assert "total_photos_received" in stats
        print(f"✓ Dashboard telemetry verified: {stats['connection_status']}, {stats['total_photos_received']} photos ingested", flush=True)

    print("\n🎉 ALL 10 SNAPSYNC BACKEND E2E TESTS PASSED SUCCESSFULLY!\n", flush=True)

if __name__ == "__main__":
    asyncio.run(run_all_tests())
