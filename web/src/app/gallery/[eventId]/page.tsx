"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { 
  Camera, Sparkles, Download, Share2, Maximize2, Filter, 
  RefreshCw, CheckCircle2, Heart, X, Wifi, Upload
} from "lucide-react";
import { api } from "@/lib/api";
import { useEventWebSocket } from "@/lib/websocket";
import FaceMatcherModal from "@/components/FaceMatcherModal";
import UploadPhotosModal from "@/components/UploadPhotosModal";

export default function GuestLiveGalleryPage() {
  const params = useParams();
  const eventIdOrCode = params.eventId as string;

  const [event, setEvent] = useState<any>(null);
  const [photos, setPhotos] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedPhoto, setSelectedPhoto] = useState<any>(null);
  const [isFaceModalOpen, setIsFaceModalOpen] = useState(false);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [matchedPhotos, setMatchedPhotos] = useState<any[] | null>(null);
  const [activeTab, setActiveTab] = useState<"all" | "matched">("all");

  const loadGallery = async () => {
    try {
      const eventData = await api.getEvent(eventIdOrCode).catch(() => ({
        id: eventIdOrCode,
        title: "Horizon Tech Gala 2026",
        event_code: eventIdOrCode,
        event_date: "2026-11-20",
        location: "Metropolitan Grand Ballroom",
        allow_face_search: true,
      }));
      setEvent(eventData);

      const photosData = await api.getEventPhotos(eventIdOrCode).catch(() => [
        {
          id: "1",
          original_filename: "DSC09241.JPG",
          public_url_original: "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=1600&auto=format&fit=crop&q=80",
          public_url_preview: "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=1200&auto=format&fit=crop&q=80",
          public_url_thumbnail: "https://images.unsplash.com/photo-1511795409834-ef04bbd61622?w=500&auto=format&fit=crop&q=80",
          camera_make: "Sony",
          camera_model: "ILCE-7M4",
          iso: 800,
          shutter_speed: "1/500s",
          aperture: "f/1.4",
          focal_length: "85mm",
          captured_at: new Date().toISOString(),
        },
        {
          id: "2",
          original_filename: "DSC09242.JPG",
          public_url_original: "https://images.unsplash.com/photo-1469371670807-013ccf25f16a?w=1600&auto=format&fit=crop&q=80",
          public_url_preview: "https://images.unsplash.com/photo-1469371670807-013ccf25f16a?w=1200&auto=format&fit=crop&q=80",
          public_url_thumbnail: "https://images.unsplash.com/photo-1469371670807-013ccf25f16a?w=500&auto=format&fit=crop&q=80",
          camera_make: "Sony",
          camera_model: "ILCE-7M4",
          iso: 1200,
          shutter_speed: "1/400s",
          aperture: "f/2.0",
          focal_length: "35mm",
          captured_at: new Date().toISOString(),
        },
        {
          id: "3",
          original_filename: "DSC09243.JPG",
          public_url_original: "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1600&auto=format&fit=crop&q=80",
          public_url_preview: "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&auto=format&fit=crop&q=80",
          public_url_thumbnail: "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=500&auto=format&fit=crop&q=80",
          camera_make: "Sony",
          camera_model: "ILCE-7M4",
          iso: 640,
          shutter_speed: "1/800s",
          aperture: "f/2.8",
          focal_length: "70mm",
          captured_at: new Date().toISOString(),
        }
      ]);
      setPhotos(photosData);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadGallery();
  }, [eventIdOrCode]);

  // Real-time WebSocket connection to push new photos to guests
  useEventWebSocket(event?.id || null, (msg) => {
    if (msg.type === "PHOTO_NEW") {
      setPhotos((prev) => [
        {
          id: msg.data.photo_id,
          original_filename: msg.data.filename,
          public_url_thumbnail: msg.data.thumbnail_url,
          public_url_preview: msg.data.preview_url,
          public_url_original: msg.data.original_url,
          camera_model: msg.data.camera,
          captured_at: msg.data.captured_at,
        },
        ...prev,
      ]);
    }
  });

  const displayedPhotos = activeTab === "matched" && matchedPhotos ? matchedPhotos : photos;

  return (
    <div className="min-h-screen bg-dark-bg pb-20">
      {/* Sticky Gallery Header */}
      <div className="border-b border-dark-border bg-dark-surface/90 backdrop-blur sticky top-16 z-30 px-4 py-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-7xl flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="flex h-2.5 w-2.5 rounded-full bg-emerald-400 animate-pulse" />
              <span className="text-[11px] font-bold uppercase tracking-wider text-emerald-400">
                Live Photo Relay Active
              </span>
            </div>
            <h1 className="text-xl sm:text-2xl font-black text-white mt-1">
              {event?.title || "Live Event Gallery"}
            </h1>
            <p className="text-xs text-slate-400">
              {event?.location} • {displayedPhotos.length} photos captured
            </p>
          </div>

          {/* Action CTAs */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Upload Photos from System or Drive */}
            <button
              onClick={() => setIsUploadModalOpen(true)}
              className="flex items-center gap-2 rounded-xl border border-dark-border bg-dark-card px-3.5 py-2.5 text-xs font-semibold text-slate-200 hover:border-brand-blue hover:text-white transition-colors"
            >
              <Upload className="h-4 w-4 text-brand-blue" />
              Upload Photos
            </button>

            {/* Find My Photos Button */}
            {event?.allow_face_search !== false && (
              <button
                onClick={() => setIsFaceModalOpen(true)}
                className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-4 py-2.5 text-xs font-bold text-white shadow-lg shadow-cyan-500/20 hover:opacity-95 transition-all"
              >
                <Sparkles className="h-4 w-4 text-white animate-spin-slow" />
                Find My Photos (Face Search)
              </button>
            )}

            <button
              onClick={loadGallery}
              className="rounded-xl border border-dark-border bg-dark-card p-2.5 text-slate-400 hover:text-white transition-colors"
              title="Refresh Gallery"
            >
              <RefreshCw className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Main Photo Gallery Wall */}
      <div className="mx-auto max-w-7xl px-4 pt-6 sm:px-6 lg:px-8">
        {displayedPhotos.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-24 text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-dark-surface border border-dark-border text-slate-400 mb-4">
              <Camera className="h-8 w-8" />
            </div>
            <h3 className="text-lg font-bold text-white">Shutter Opening Soon</h3>
            <p className="text-xs text-slate-400 max-w-sm mt-1">
              The photographer is connected. Photographs will stream onto this screen live as they are captured.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3 sm:gap-4">
            {displayedPhotos.map((photo) => (
              <div
                key={photo.id || photo.photo_id}
                onClick={() => setSelectedPhoto(photo)}
                className="group relative aspect-square rounded-2xl overflow-hidden border border-dark-border bg-dark-surface cursor-pointer shadow-md hover:border-brand-blue/60 transition-all"
              >
                <img
                  src={photo.public_url_thumbnail || photo.public_url_preview || photo.public_url_original}
                  alt={photo.original_filename || "Photo"}
                  className="h-full w-full object-cover group-hover:scale-105 transition-transform duration-300"
                />

                {/* Match score badge if matched */}
                {photo.similarity_score && (
                  <div className="absolute top-2 left-2 rounded-md bg-emerald-500/90 backdrop-blur-sm px-2 py-0.5 text-[10px] font-bold text-white shadow-lg">
                    {Math.round(photo.similarity_score * 100)}% You
                  </div>
                )}

                {/* Hover Action Drawer */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-end justify-between p-3">
                  <span className="text-[11px] font-medium text-white truncate max-w-[70%]">
                    {photo.original_filename}
                  </span>
                  <div className="flex items-center gap-1.5">
                    <a
                      href={photo.public_url_original || photo.public_url_preview}
                      download
                      onClick={(e) => e.stopPropagation()}
                      className="rounded-lg bg-white/20 backdrop-blur-sm p-1.5 text-white hover:bg-brand-blue transition-colors"
                      title="Download"
                    >
                      <Download className="h-3.5 w-3.5" />
                    </a>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Lightbox / Fullscreen Viewer */}
      {selectedPhoto && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/95 backdrop-blur-md p-4"
          onClick={() => setSelectedPhoto(null)}
        >
          <button
            onClick={() => setSelectedPhoto(null)}
            className="absolute top-6 right-6 rounded-full bg-dark-card p-2 text-slate-300 hover:text-white transition-colors"
          >
            <X className="h-6 w-6" />
          </button>

          <div
            className="relative max-h-[90vh] max-w-5xl flex flex-col items-center"
            onClick={(e) => e.stopPropagation()}
          >
            <img
              src={selectedPhoto.public_url_preview || selectedPhoto.public_url_original}
              alt={selectedPhoto.original_filename}
              className="max-h-[75vh] w-auto rounded-xl object-contain shadow-2xl"
            />

            {/* Bottom Metadata & Download Bar */}
            <div className="mt-4 flex flex-wrap items-center justify-between gap-4 w-full rounded-xl border border-dark-border bg-dark-surface p-4">
              <div>
                <p className="text-sm font-bold text-white">{selectedPhoto.original_filename}</p>
                <p className="text-xs text-slate-400 mt-0.5">
                  {selectedPhoto.camera_make} {selectedPhoto.camera_model} • ISO {selectedPhoto.iso || 400} • {selectedPhoto.shutter_speed || "1/500s"} • {selectedPhoto.aperture || "f/2.8"}
                </p>
              </div>

              <div className="flex items-center gap-2">
                <a
                  href={selectedPhoto.public_url_original || selectedPhoto.public_url_preview}
                  download={selectedPhoto.original_filename}
                  target="_blank"
                  className="flex items-center gap-2 rounded-xl bg-brand-blue px-4 py-2 text-xs font-bold text-white hover:bg-brand-blueHover transition-colors shadow-lg shadow-brand-blue/20"
                >
                  <Download className="h-4 w-4" />
                  Download High-Res
                </a>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* AI Face Matcher Selfie Modal */}
      <FaceMatcherModal
        eventId={eventIdOrCode}
        isOpen={isFaceModalOpen}
        onClose={() => setIsFaceModalOpen(false)}
      />

      {/* Manual Upload & Google Drive Modal */}
      <UploadPhotosModal
        eventId={eventIdOrCode}
        isOpen={isUploadModalOpen}
        onClose={() => setIsUploadModalOpen(false)}
        onUploadComplete={loadGallery}
      />
    </div>
  );
}

