from datetime import datetime, date, timedelta
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.core.config import settings
from app.core.database import Base
from app.core.security import get_password_hash
from app.models.models import User, PhotographerProfile, Event, Camera, StorageUsage, Subscription
from app.services.event_service import event_service

def seed_data():
    sync_url = settings.DATABASE_URL.replace("+aiosqlite", "")
    engine = create_engine(sync_url)
    Base.metadata.create_all(engine)
    Session = sessionmaker(bind=engine)

    with Session() as db:
        # Check if already seeded
        existing = db.query(User).filter(User.email == "demo@snapsync.io").first()
        if existing:
            print("Demo user already exists.")
            return

        print("Seeding SnapSync demo data...")

        # Create demo photographer
        photographer = User(
            email="demo@snapsync.io",
            hashed_password=get_password_hash("SnapSync2026!"),
            full_name="Alex Sterling",
            role="photographer",
            is_active=True,
            is_verified=True
        )
        db.add(photographer)
        db.flush()

        # Create profile
        profile = PhotographerProfile(
            user_id=photographer.id,
            company_name="Sterling Live Media",
            phone="+1 (555) 234-5678",
            website="https://sterlinglive.com"
        )
        db.add(profile)

        # Storage & Subscription
        storage = StorageUsage(photographer_id=photographer.id, total_bytes_used=1258291200)
        db.add(storage)

        sub = Subscription(
            photographer_id=photographer.id,
            tier="pro",
            current_period_end=datetime.utcnow() + timedelta(days=365)
        )
        db.add(sub)

        # Sample Cameras
        cam1 = Camera(
            photographer_id=photographer.id,
            make="Sony",
            model="ILCE-7M4 (A7 IV)",
            serial_number="3849120",
            protocol="ftp",
            default_ftp_username="snapsync",
            default_ftp_port=2121
        )
        cam2 = Camera(
            photographer_id=photographer.id,
            make="Canon",
            model="EOS R5 Mark II",
            serial_number="8912340",
            protocol="ftp",
            default_ftp_username="snapsync",
            default_ftp_port=2121
        )
        db.add_all([cam1, cam2])

        # Sample Event
        event_code = "GALA-2026"
        event = Event(
            photographer_id=photographer.id,
            title="Horizon Tech Gala 2026",
            event_code=event_code,
            event_date=date.today(),
            location="Metropolitan Grand Ballroom",
            description="Annual executive summit and evening awards gala with live photo relay.",
            is_public=True,
            allow_face_search=True,
            allow_guest_downloads=True,
            qr_code_url=event_service.generate_event_qr_base64(event_code),
            status="active"
        )
        db.add(event)

        # Admin user
        admin = User(
            email="admin@snapsync.io",
            hashed_password=get_password_hash("AdminMaster2026!"),
            full_name="System Administrator",
            role="admin",
            is_active=True,
            is_verified=True
        )
        db.add(admin)

        db.commit()
        print("Database seeding completed successfully.")
        print("Photographer: demo@snapsync.io / SnapSync2026!")
        print("Admin: admin@snapsync.io / AdminMaster2026!")
        print(f"Sample Active Event Code: {event_code}")

if __name__ == "__main__":
    seed_data()
