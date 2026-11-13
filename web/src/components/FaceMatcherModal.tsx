"use client";

import { useState, useRef, useEffect } from "react";
import { X, Camera, Upload, Sparkles, CheckCircle2, AlertCircle, Download, RefreshCw, Eye } from "lucide-react";
import { api, formatImageUrl } from "@/lib/api";

interface FaceMatcherModalProps {
  eventId: string;
  isOpen: boolean;
  onClose: () => void;
  onMatchesFound?: (matches: any[]) => void;
}

export default function FaceMatcherModal({ eventId, isOpen, onClose, onMatchesFound }: FaceMatcherModalProps) {
  const [mode, setMode] = useState<"choose" | "webcam" | "upload">("choose");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [results, setResults] = useState<any[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Webcam elements
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Stop webcam stream when closing or switching mode
  const stopWebcam = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
  };

  useEffect(() => {
    if (!isOpen) {
      stopWebcam();
      setMode("choose");
      setSelectedFile(null);
      setPreviewUrl(null);
      setResults(null);
      setError(null);
    }
  }, [isOpen]);

  const startWebcam = async () => {
    try {
      setError(null);
      setMode("webcam");
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "user", width: { ideal: 640 }, height: { ideal: 640 } },
        audio: false,
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
    } catch (err) {
      setError("Unable to access camera. Please allow camera permissions or upload a selfie file instead.");
      setMode("choose");
    }
  };

  const captureWebcamSnapshot = () => {
    if (!videoRef.current) return;
    const video = videoRef.current;
    const canvas = document.createElement("canvas");
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 640;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    stopWebcam();

    canvas.toBlob((blob) => {
      if (blob) {
        const file = new File([blob], `selfie_${Date.now()}.jpg`, { type: "image/jpeg" });
        setSelectedFile(file);
        setPreviewUrl(URL.createObjectURL(file));
        setMode("choose");
      }
    }, "image/jpeg", 0.95);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      stopWebcam();
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
      setResults(null);
      setError(null);
      setMode("choose");
    }
  };

  const handleSearch = async () => {
    if (!selectedFile) return;
    setIsSearching(true);
    setError(null);
    setResults(null);

    try {
      const data = await api.searchPhotosByFace(eventId, selectedFile, 0.65);
      setResults(data.matches || []);
      if (!data.matches || data.matches.length === 0) {
        setError("No photos found matching your face in this event yet. Photos might still be uploading from the cameras.");
      }
    } catch (err: any) {
      setError(err.message || "Face matching failed. Please ensure your selfie is clear and well-lit.");
    } finally {
      setIsSearching(false);
    }
  };

  const handleApplyToGallery = () => {
    if (results && onMatchesFound) {
      onMatchesFound(results);
      onClose();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-sm p-4 overflow-y-auto">
      <div className="relative w-full max-w-2xl rounded-2xl border border-dark-border bg-dark-surface p-6 shadow-2xl">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-dark-card hover:text-white transition-colors"
        >
          <X className="h-5 w-5" />
        </button>

        {/* Header */}
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-blue/20 text-brand-blue">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">Find My Photos with AI Face Search</h2>
            <p className="text-xs text-slate-400">Take a live selfie or upload a photo to find every event photograph you appear in.</p>
          </div>
        </div>

        {/* Mode Selector & Camera Viewfinder */}
        <div className="mt-6">
          <input
            type="file"
            ref={fileInputRef}
            accept="image/*"
            onChange={handleFileChange}
            className="hidden"
          />

          {mode === "webcam" ? (
            <div className="flex flex-col items-center justify-center rounded-2xl border border-dark-border bg-dark-bg p-4">
              <div className="relative aspect-square w-full max-w-xs overflow-hidden rounded-2xl border-2 border-brand-cyan shadow-lg shadow-brand-cyan/10">
                <video
                  ref={videoRef}
                  autoPlay
                  playsInline
                  muted
                  className="h-full w-full object-cover -scale-x-100"
                />
                {/* Oval Face Guide */}
                <div className="absolute inset-0 pointer-events-none border-2 border-dashed border-white/50 rounded-full m-8" />
              </div>
              <div className="mt-4 flex items-center gap-3">
                <button
                  onClick={captureWebcamSnapshot}
                  className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-5 py-2.5 text-xs font-bold text-white shadow-lg shadow-cyan-500/25 hover:opacity-95 transition-all"
                >
                  <Camera className="h-4 w-4" />
                  Capture Selfie
                </button>
                <button
                  onClick={() => {
                    stopWebcam();
                    setMode("choose");
                  }}
                  className="rounded-xl border border-dark-border px-4 py-2.5 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
                >
                  Cancel
                </button>
              </div>
            </div>
          ) : !previewUrl ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Option 1: Live Selfie */}
              <div
                onClick={startWebcam}
                className="group flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-dark-border bg-dark-card/60 p-8 text-center cursor-pointer hover:border-brand-blue hover:bg-dark-card transition-all"
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-blue/10 text-brand-blue group-hover:scale-110 transition-transform mb-3">
                  <Camera className="h-6 w-6" />
                </div>
                <p className="text-sm font-bold text-white">Take Live Selfie</p>
                <p className="mt-1 text-xs text-slate-400">Use your device webcam or front camera</p>
              </div>

              {/* Option 2: Upload File */}
              <div
                onClick={() => fileInputRef.current?.click()}
                className="group flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-dark-border bg-dark-card/60 p-8 text-center cursor-pointer hover:border-brand-cyan hover:bg-dark-card transition-all"
              >
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-cyan/10 text-brand-cyan group-hover:scale-110 transition-transform mb-3">
                  <Upload className="h-6 w-6" />
                </div>
                <p className="text-sm font-bold text-white">Upload Existing Selfie</p>
                <p className="mt-1 text-xs text-slate-400">Choose JPEG, PNG from your files</p>
              </div>
            </div>
          ) : (
            /* Selfie Preview & Action Bar */
            <div className="flex flex-col sm:flex-row items-center gap-5 rounded-2xl border border-dark-border bg-dark-card p-4">
              <img
                src={previewUrl}
                alt="Your selfie"
                className="h-28 w-28 rounded-2xl object-cover border-2 border-brand-cyan shadow-md shadow-brand-cyan/20"
              />
              <div className="flex-1 text-center sm:text-left">
                <p className="text-sm font-bold text-white">Selfie Ready for Facial Embedding</p>
                <p className="text-xs text-slate-400 mt-1">
                  Our deep learning engine will scan all event photographs for your facial characteristics.
                </p>
                <div className="mt-4 flex flex-wrap items-center justify-center sm:justify-start gap-2.5">
                  <button
                    onClick={handleSearch}
                    disabled={isSearching}
                    className="flex items-center gap-2 rounded-xl bg-gradient-to-r from-blue-600 to-cyan-500 px-5 py-2.5 text-xs font-bold text-white shadow-lg shadow-cyan-500/20 hover:opacity-95 disabled:opacity-50 transition-all"
                  >
                    {isSearching ? (
                      <>
                        <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                        Scanning Event Photos...
                      </>
                    ) : (
                      <>
                        <Sparkles className="h-3.5 w-3.5" />
                        Search My Photos
                      </>
                    )}
                  </button>
                  <button
                    onClick={() => {
                      setPreviewUrl(null);
                      setSelectedFile(null);
                      setResults(null);
                      setError(null);
                    }}
                    className="rounded-xl border border-dark-border bg-dark-surface px-3.5 py-2.5 text-xs font-medium text-slate-300 hover:text-white transition-colors"
                  >
                    Retake Selfie
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Error message */}
        {error && (
          <div className="mt-4 flex items-center gap-2.5 rounded-xl bg-amber-500/10 border border-amber-500/30 p-3.5 text-xs text-amber-300">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Search Results */}
        {results && results.length > 0 && (
          <div className="mt-6 border-t border-dark-border pt-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <CheckCircle2 className="h-4 w-4 text-emerald-400" />
                Found {results.length} Photo{results.length === 1 ? "" : "s"} Matching You!
              </h3>

              {onMatchesFound && (
                <button
                  onClick={handleApplyToGallery}
                  className="flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-bold text-white shadow-lg shadow-emerald-600/20 hover:bg-emerald-500 transition-colors"
                >
                  <Eye className="h-3.5 w-3.5" />
                  View These in Gallery Wall
                </button>
              )}
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 max-h-72 overflow-y-auto pr-1">
              {results.map((photo) => (
                <div
                  key={photo.photo_id}
                  className="group relative rounded-xl overflow-hidden border border-dark-border bg-dark-bg aspect-square"
                >
                  <img
                    src={formatImageUrl(photo.public_url_thumbnail || photo.public_url_preview || photo.public_url_original)}
                    alt={photo.original_filename}
                    className="h-full w-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute top-2 left-2 rounded-md bg-emerald-500/90 backdrop-blur-sm px-2 py-0.5 text-[10px] font-bold text-white shadow">
                    {Math.round(photo.similarity_score * 100)}% Match
                  </div>
                  <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-end justify-between p-2.5">
                    <span className="text-[10px] text-white truncate max-w-[70%]">
                      {photo.original_filename}
                    </span>
                    <a
                      href={formatImageUrl(photo.public_url_original || photo.public_url_preview)}
                      download
                      target="_blank"
                      rel="noreferrer"
                      className="rounded-lg bg-white/20 p-1.5 text-white hover:bg-brand-blue transition-colors"
                    >
                      <Download className="h-3 w-3" />
                    </a>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
