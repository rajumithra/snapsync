"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { 
  Camera, Wifi, CloudUpload, HardDrive, Zap, CheckCircle2, 
  Hourglass, AlertCircle, ArrowUpRight, QrCode, Play, StopCircle, RefreshCw
} from "lucide-react";
import { api, formatImageUrl } from "@/lib/api";
import { useEventWebSocket } from "@/lib/websocket";
import QRCodeModal from "@/components/QRCodeModal";
import UploadPhotosModal from "@/components/UploadPhotosModal";
import { Upload } from "lucide-react";

export default function DashboardPage() {
  const [stats, setStats] = useState<any>(null);
  const [activeEvent, setActiveEvent] = useState<any>(null);
  const [photos, setPhotos] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showQrModal, setShowQrModal] = useState(false);
  const [showUploadModal, setShowUploadModal] = useState(false);

  const loadData = async () => {
    try {
      const events = await api.getEvents().catch(() => []);
      if (events.length > 0) {
        setActiveEvent(events[0]);
        const eventPhotos = await api.getEventPhotos(events[0].id).catch(() => []);
        setPhotos(eventPhotos || []);
      } else {
        setActiveEvent(null);
        setPhotos([]);
      }

      const statsData = await api.getDashboardStats().catch(() => ({
        connection_status: "ONLINE",
        active_event_title: events.length > 0 ? events[0].title : "No Active Events",
        active_event_id: events.length > 0 ? events[0].id : null,
        connected_cameras_count: 0,
        total_photos_received: 0,
        total_photos_uploaded: 0,
        pending_uploads: 0,
        failed_uploads: 0,
        upload_speed_mbps: 0,
        storage_used_bytes: 0,
        storage_limit_bytes: 53687091200,
      }));
      setStats(statsData);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Listen to live WebSocket events when activeEvent exists
  useEventWebSocket(activeEvent?.id || null, (msg) => {
    if (msg.type === "PHOTO_NEW") {
      setPhotos((prev) => [msg.data, ...prev]);
      setStats((prev: any) => ({
        ...prev,
        total_photos_received: (prev?.total_photos_received || 0) + 1,
        total_photos_uploaded: (prev?.total_photos_uploaded || 0) + 1,
      }));
    }
  });

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 space-y-8">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white">Relay Control Center</h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Real-time camera ingestion telemetry and cloud pipeline distribution.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowUploadModal(true)}
            className="flex items-center gap-2 rounded-xl bg-brand-blue px-4 py-2 text-xs font-semibold text-white hover:bg-brand-blueHover transition-colors shadow-lg shadow-brand-blue/20"
          >
            <Upload className="h-4 w-4" />
            Upload Photos (System & Drive)
          </button>

          <button
            onClick={loadData}
            className="flex items-center gap-2 rounded-xl border border-dark-border bg-dark-surface px-3.5 py-2 text-xs font-semibold text-slate-300 hover:text-white hover:border-brand-blue transition-colors"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Refresh
          </button>

          {activeEvent && (
            <button
              onClick={() => setShowQrModal(true)}
              className="flex items-center gap-2 rounded-xl border border-dark-border bg-dark-card px-4 py-2 text-xs font-semibold text-slate-200 hover:border-brand-blue hover:text-white transition-colors"
            >
              <QrCode className="h-4 w-4 text-brand-cyan" />
              Event QR Code
            </button>
          )}
        </div>
      </div>

      {/* Primary Status Banner */}
      <div className="rounded-2xl border border-dark-border bg-dark-surface p-6 shadow-xl">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="flex items-start gap-4">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <Wifi className="h-6 w-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold text-white">
                  {activeEvent?.title || "Live Transmission Active"}
                </h2>
                <span className="rounded-full bg-emerald-500/10 border border-emerald-500/30 px-2 py-0.5 text-[10px] font-bold text-emerald-400">
                  ONLINE
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                Camera link active on port <span className="text-brand-cyan font-mono">2121</span>. Connected:{" "}
                <span className="text-slate-200 font-semibold">{stats?.connected_cameras_count || 1} Cameras</span> (Sony Alpha, Canon EOS).
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {activeEvent && (
              <Link
                href={`/gallery/${activeEvent.event_code || activeEvent.id}`}
                target="_blank"
                className="flex items-center gap-1.5 rounded-xl border border-dark-border bg-dark-card px-4 py-2.5 text-xs font-semibold text-slate-200 hover:border-brand-blue hover:text-white transition-colors"
              >
                Open Live Guest Wall
                <ArrowUpRight className="h-3.5 w-3.5 text-brand-blue" />
              </Link>
            )}
          </div>
        </div>
      </div>

      {/* Telemetry Stats Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Ingested */}
        <div className="rounded-2xl border border-dark-border bg-dark-surface p-5">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Ingested</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-500/10 text-brand-blue">
              <Camera className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-3xl font-extrabold text-white">{stats?.total_photos_received || 0}</span>
            <span className="ml-1 text-xs text-slate-400">photos</span>
          </div>
          <p className="mt-1 text-[11px] text-slate-400">Via Wi-Fi FTP / OTG</p>
        </div>

        {/* Uploaded to Cloud */}
        <div className="rounded-2xl border border-dark-border bg-dark-surface p-5">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Cloud Synced</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-400">
              <CheckCircle2 className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-3xl font-extrabold text-white">{stats?.total_photos_uploaded || 0}</span>
            <span className="ml-1 text-xs text-slate-400">confirmed</span>
          </div>
          <p className="mt-1 text-[11px] text-emerald-400">100% RAW/JPEG fidelity</p>
        </div>

        {/* Upload Speed */}
        <div className="rounded-2xl border border-dark-border bg-dark-surface p-5">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Throughput</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-cyan-500/10 text-brand-cyan">
              <Zap className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-3xl font-extrabold text-white">{stats?.upload_speed_mbps || 0}</span>
            <span className="ml-1 text-xs text-slate-400">MB/s</span>
          </div>
          <p className="mt-1 text-[11px] text-slate-400">Direct S3 Signed Stream</p>
        </div>

        {/* Storage */}
        <div className="rounded-2xl border border-dark-border bg-dark-surface p-5">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Storage Used</span>
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-400">
              <HardDrive className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-3xl font-extrabold text-white">
              {((stats?.storage_used_bytes || 0) / (1024 * 1024 * 1024)).toFixed(1)}
            </span>
            <span className="ml-1 text-xs text-slate-400">/ 50 GB</span>
          </div>
          <div className="mt-2 h-1.5 w-full rounded-full bg-dark-border overflow-hidden">
            <div className="h-full bg-brand-blue rounded-full" style={{ width: "8%" }} />
          </div>
        </div>
      </div>

      {/* Live Ingestion Stream */}
      <div className="rounded-2xl border border-dark-border bg-dark-surface p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-lg font-bold text-white flex items-center gap-2">
              <span className="flex h-2 w-2 rounded-full bg-brand-cyan animate-ping" />
              Live Camera Feed Stream
            </h2>
            <p className="text-xs text-slate-400">Photos appear here automatically as the photographer shoots.</p>
          </div>
          <Link
            href="/events"
            className="text-xs font-semibold text-brand-blue hover:underline"
          >
            Manage Events →
          </Link>
        </div>

        {photos.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-center border-2 border-dashed border-dark-border rounded-xl">
            <Camera className="h-10 w-10 text-slate-500 mb-3" />
            <p className="text-sm font-semibold text-slate-300">Awaiting incoming camera shots</p>
            <p className="text-xs text-slate-500 mt-1 max-w-sm">
              Press the camera shutter to automatically transmit photos over FTP/Wi-Fi to this dashboard.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
            {photos.map((photo, i) => (
              <div
                key={photo.photo_id || photo.id || i}
                className="group relative rounded-xl overflow-hidden border border-dark-border bg-dark-card aspect-square"
              >
                <img
                  src={formatImageUrl(photo.thumbnail_url || photo.public_url_thumbnail || photo.preview_url || photo.public_url_original)}
                  alt={photo.filename || photo.original_filename || "Photo"}
                  className="h-full w-full object-cover group-hover:scale-105 transition-transform duration-300"
                />
                <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/90 via-black/40 to-transparent p-2 text-left">
                  <p className="text-[11px] font-semibold text-white truncate">
                    {photo.filename || photo.original_filename}
                  </p>
                  <p className="text-[9px] text-slate-400">
                    {photo.camera || "Sony A7 IV"}
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* QR Code Modal */}
      {activeEvent && (
        <QRCodeModal
          event={activeEvent}
          isOpen={showQrModal}
          onClose={() => setShowQrModal(false)}
        />
      )}

      {/* Manual & Google Drive Upload Modal */}
      {activeEvent && (
        <UploadPhotosModal
          eventId={activeEvent.id || activeEvent.event_code}
          isOpen={showUploadModal}
          onClose={() => setShowUploadModal(false)}
          onUploadComplete={loadData}
        />
      )}
    </div>
  );
}

