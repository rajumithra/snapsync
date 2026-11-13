"use client";

import { useState, useRef } from "react";
import { X, Camera, Upload, Sparkles, CheckCircle2, AlertCircle, Download } from "lucide-react";
import { api } from "@/lib/api";

interface FaceMatcherModalProps {
  eventId: string;
  isOpen: boolean;
  onClose: () => void;
}

export default function FaceMatcherModal({ eventId, isOpen, onClose }: FaceMatcherModalProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [results, setResults] = useState<any[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
      setResults(null);
      setError(null);
    }
  };

  const handleSearch = async () => {
    if (!selectedFile) return;
    setIsSearching(true);
    setError(null);

    try {
      const data = await api.searchPhotosByFace(eventId, selectedFile, 0.65);
      setResults(data.matches);
      if (data.matches.length === 0) {
        setError("No photos found matching your face in this event yet. Check back soon as new photos are uploaded live!");
      }
    } catch (err: any) {
      setError(err.message || "Face matching failed. Please try a clear, front-facing selfie.");
    } finally {
      setIsSearching(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 overflow-y-auto">
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
            <p className="text-xs text-slate-400">Take or upload a selfie to instantly find all event photos you appear in.</p>
          </div>
        </div>

        {/* Selfie Upload Area */}
        <div className="mt-6">
          <input
            type="file"
            ref={fileInputRef}
            accept="image/*"
            capture="user"
            onChange={handleFileChange}
            className="hidden"
          />

          {!previewUrl ? (
            <div
              onClick={() => fileInputRef.current?.click()}
              className="flex flex-col items-center justify-center rounded-xl border-2 border-dashed border-dark-border bg-dark-bg/60 p-8 text-center cursor-pointer hover:border-brand-blue transition-colors"
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-full bg-dark-card text-brand-blue mb-3">
                <Camera className="h-6 w-6" />
              </div>
              <p className="text-sm font-semibold text-white">Take a Selfie or Upload Photo</p>
              <p className="mt-1 text-xs text-slate-400">Front-facing, well-lit photo works best</p>
            </div>
          ) : (
            <div className="flex flex-col sm:flex-row items-center gap-4 rounded-xl border border-dark-border bg-dark-card p-4">
              <img
                src={previewUrl}
                alt="Your selfie"
                className="h-28 w-28 rounded-xl object-cover border border-brand-blue/40"
              />
              <div className="flex-1 text-center sm:text-left">
                <p className="text-sm font-semibold text-white">Selfie Ready for Analysis</p>
                <p className="text-xs text-slate-400 mt-0.5">We will generate facial embeddings to search the live event gallery.</p>
                <div className="mt-3 flex items-center justify-center sm:justify-start gap-2">
                  <button
                    onClick={handleSearch}
                    disabled={isSearching}
                    className="flex items-center gap-2 rounded-lg bg-brand-blue px-4 py-2 text-xs font-semibold text-white hover:bg-brand-blueHover disabled:opacity-50 transition-colors"
                  >
                    {isSearching ? (
                      <>
                        <div className="h-3 w-3 animate-spin rounded-full border-2 border-white border-t-transparent" />
                        Scanning Event Photos...
                      </>
                    ) : (
                      <>
                        <Sparkles className="h-4 w-4" />
                        Search My Photos
                      </>
                    )}
                  </button>
                  <button
                    onClick={() => {
                      setPreviewUrl(null);
                      setSelectedFile(null);
                      setResults(null);
                    }}
                    className="rounded-lg border border-dark-border px-3 py-2 text-xs font-medium text-slate-400 hover:text-white transition-colors"
                  >
                    Retake
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Error message */}
        {error && (
          <div className="mt-4 flex items-center gap-2 rounded-lg bg-amber-500/10 border border-amber-500/30 p-3 text-xs text-amber-300">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Search Results */}
        {results && results.length > 0 && (
          <div className="mt-6 border-t border-dark-border pt-4">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <CheckCircle2 className="h-4 w-4 text-emerald-400" />
                Found {results.length} Photos Featuring You!
              </h3>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 max-h-72 overflow-y-auto pr-1">
              {results.map((photo) => (
                <div
                  key={photo.photo_id}
                  className="group relative rounded-xl overflow-hidden border border-dark-border bg-dark-bg aspect-square"
                >
                  <img
                    src={photo.public_url_thumbnail || photo.public_url_preview || photo.public_url_original}
                    alt={photo.original_filename}
                    className="h-full w-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute top-2 left-2 rounded-md bg-emerald-500/90 backdrop-blur-sm px-1.5 py-0.5 text-[10px] font-bold text-white">
                    {Math.round(photo.similarity_score * 100)}% Match
                  </div>
                  <a
                    href={photo.public_url_original || photo.public_url_preview}
                    target="_blank"
                    download
                    className="absolute bottom-2 right-2 rounded-lg bg-dark-bg/80 backdrop-blur-sm p-1.5 text-white opacity-0 group-hover:opacity-100 transition-opacity hover:bg-brand-blue"
                    title="Download Photo"
                  >
                    <Download className="h-3.5 w-3.5" />
                  </a>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
