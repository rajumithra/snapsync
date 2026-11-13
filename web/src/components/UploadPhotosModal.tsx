"use client";

import { useState, useRef } from "react";
import { 
  X, Upload, HardDrive, CheckCircle2, AlertCircle, FileImage, 
  ArrowRight, Sparkles, FolderDown, Link2
} from "lucide-react";
import { api } from "@/lib/api";

interface UploadPhotosModalProps {
  eventId: string;
  isOpen: boolean;
  onClose: () => void;
  onUploadComplete?: () => void;
}

export default function UploadPhotosModal({
  eventId,
  isOpen,
  onClose,
  onUploadComplete,
}: UploadPhotosModalProps) {
  const [activeTab, setActiveTab] = useState<"system" | "drive">("system");
  
  // Local files state
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  
  // Google Drive state
  const [driveUrl, setDriveUrl] = useState("");
  const [driveImporting, setDriveImporting] = useState(false);
  
  // Status feedback
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const filesArray = Array.from(e.target.files);
      setSelectedFiles((prev) => [...prev, ...filesArray]);
      setSuccessMessage(null);
      setErrorMessage(null);
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (e.dataTransfer.files) {
      const filesArray = Array.from(e.dataTransfer.files);
      setSelectedFiles((prev) => [...prev, ...filesArray]);
      setSuccessMessage(null);
      setErrorMessage(null);
    }
  };

  const removeFile = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSystemUpload = async () => {
    if (selectedFiles.length === 0) return;
    setUploading(true);
    setUploadProgress(15);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      setUploadProgress(50);
      await api.manualUploadPhotos(eventId, selectedFiles);
      setUploadProgress(100);
      setSuccessMessage(`Successfully uploaded ${selectedFiles.length} photo(s) to event gallery!`);
      setSelectedFiles([]);
      if (onUploadComplete) onUploadComplete();
    } catch (err: any) {
      setErrorMessage(err.message || "Upload failed. Please try again.");
    } finally {
      setUploading(false);
    }
  };

  const handleDriveImport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!driveUrl.trim()) return;
    setDriveImporting(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      await api.importPhotosFromDrive(eventId, driveUrl.trim());
      setSuccessMessage("Successfully imported photograph from Google Drive into event gallery!");
      setDriveUrl("");
      if (onUploadComplete) onUploadComplete();
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to import from Google Drive. Ensure link has 'Anyone with the link' sharing enabled.");
    } finally {
      setDriveImporting(false);
    }
  };

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
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Upload className="h-5 w-5 text-brand-blue" />
            Upload & Ingest Photos
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Manually upload high-res photos from your computer or import directly from Google Drive.
          </p>
        </div>

        {/* Tabs */}
        <div className="mt-6 flex border-b border-dark-border">
          <button
            onClick={() => setActiveTab("system")}
            className={`flex items-center gap-2 border-b-2 px-4 py-2.5 text-xs font-semibold transition-colors ${
              activeTab === "system"
                ? "border-brand-blue text-brand-blue"
                : "border-transparent text-slate-400 hover:text-white"
            }`}
          >
            <HardDrive className="h-4 w-4" />
            Upload from Computer
          </button>

          <button
            onClick={() => setActiveTab("drive")}
            className={`flex items-center gap-2 border-b-2 px-4 py-2.5 text-xs font-semibold transition-colors ${
              activeTab === "drive"
                ? "border-brand-blue text-brand-blue"
                : "border-transparent text-slate-400 hover:text-white"
            }`}
          >
            <FolderDown className="h-4 w-4 text-emerald-400" />
            Import from Google Drive
          </button>
        </div>

        {/* Feedback Messages */}
        {successMessage && (
          <div className="mt-4 flex items-center gap-2 rounded-xl bg-emerald-500/10 border border-emerald-500/30 p-3 text-xs text-emerald-300">
            <CheckCircle2 className="h-4 w-4 shrink-0" />
            <span>{successMessage}</span>
          </div>
        )}

        {errorMessage && (
          <div className="mt-4 flex items-center gap-2 rounded-xl bg-red-500/10 border border-red-500/30 p-3 text-xs text-red-300">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Tab 1: System Upload */}
        {activeTab === "system" && (
          <div className="mt-6 space-y-4">
            <input
              type="file"
              ref={fileInputRef}
              multiple
              accept="image/*,.cr3,.arw,.nef,.dng"
              onChange={handleFileSelect}
              className="hidden"
            />

            <div
              onDragOver={(e) => e.preventDefault()}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className="flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-dark-border bg-dark-bg/60 p-8 text-center cursor-pointer hover:border-brand-blue transition-colors"
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-dark-card text-brand-blue mb-3 shadow-inner">
                <Upload className="h-6 w-6" />
              </div>
              <p className="text-sm font-semibold text-white">Drag & drop photos here, or click to browse</p>
              <p className="text-xs text-slate-400 mt-1">
                Supports JPEG, PNG, WebP, and RAW formats (Sony ARW, Canon CR3, Nikon NEF)
              </p>
            </div>

            {/* Selected files preview list */}
            {selectedFiles.length > 0 && (
              <div className="rounded-xl border border-dark-border bg-dark-card p-3 max-h-48 overflow-y-auto space-y-2">
                <div className="flex items-center justify-between text-xs font-semibold text-slate-300 pb-2 border-b border-dark-border">
                  <span>Selected Photos ({selectedFiles.length})</span>
                  <button
                    onClick={() => setSelectedFiles([])}
                    className="text-red-400 hover:underline"
                  >
                    Clear All
                  </button>
                </div>

                {selectedFiles.map((file, idx) => (
                  <div key={idx} className="flex items-center justify-between text-xs text-slate-300 py-1">
                    <div className="flex items-center gap-2 truncate max-w-[80%]">
                      <FileImage className="h-4 w-4 text-brand-blue shrink-0" />
                      <span className="truncate">{file.name}</span>
                      <span className="text-slate-500 text-[10px]">({(file.size / (1024 * 1024)).toFixed(1)} MB)</span>
                    </div>
                    <button
                      onClick={() => removeFile(idx)}
                      className="text-slate-400 hover:text-red-400"
                    >
                      <X className="h-3.5 w-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            )}

            {uploading && (
              <div className="space-y-1">
                <div className="flex justify-between text-xs text-slate-400">
                  <span>Uploading & Indexing Faces...</span>
                  <span>{uploadProgress}%</span>
                </div>
                <div className="h-2 w-full rounded-full bg-dark-border overflow-hidden">
                  <div className="h-full bg-brand-blue rounded-full transition-all duration-300" style={{ width: `${uploadProgress}%` }} />
                </div>
              </div>
            )}

            <div className="flex justify-end gap-3 pt-4 border-t border-dark-border">
              <button
                type="button"
                onClick={onClose}
                className="rounded-xl border border-dark-border px-4 py-2.5 text-xs font-semibold text-slate-300 hover:text-white"
              >
                Close
              </button>

              <button
                type="button"
                onClick={handleSystemUpload}
                disabled={selectedFiles.length === 0 || uploading}
                className="flex items-center gap-2 rounded-xl bg-brand-blue px-5 py-2.5 text-xs font-semibold text-white hover:bg-brand-blueHover disabled:opacity-50 transition-colors shadow-lg shadow-brand-blue/20"
              >
                {uploading ? (
                  <>
                    <div className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent" />
                    Processing Upload...
                  </>
                ) : (
                  <>
                    <Upload className="h-4 w-4" />
                    Upload {selectedFiles.length} Photos
                  </>
                )}
              </button>
            </div>
          </div>
        )}

        {/* Tab 2: Google Drive Import */}
        {activeTab === "drive" && (
          <form onSubmit={handleDriveImport} className="mt-6 space-y-4">
            <div className="rounded-xl border border-dark-border bg-dark-card p-4 space-y-2">
              <div className="flex items-center gap-2 text-xs font-semibold text-emerald-400">
                <FolderDown className="h-4 w-4" />
                <span>Import via Google Drive Shareable Link</span>
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">
                Paste any Google Drive public or shareable link (File or Folder). SnapSync will fetch the photograph, preserve quality, generate WebP previews, and auto-index faces.
              </p>
            </div>

            <div>
              <label className="text-xs font-medium text-slate-300">Google Drive Share Link *</label>
              <div className="relative mt-1">
                <Link2 className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
                <input
                  type="url"
                  required
                  value={driveUrl}
                  onChange={(e) => setDriveUrl(e.target.value)}
                  placeholder="https://drive.google.com/file/d/1a2b3c.../view?usp=sharing"
                  className="w-full rounded-xl border border-dark-border bg-dark-card py-2.5 pl-10 pr-4 text-xs text-white placeholder-slate-500 focus:border-brand-blue focus:outline-none"
                />
              </div>
              <p className="mt-1 text-[11px] text-slate-500">
                Tip: On Google Drive, right-click file/folder → Share → Set to "Anyone with the link".
              </p>
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-dark-border">
              <button
                type="button"
                onClick={onClose}
                className="rounded-xl border border-dark-border px-4 py-2.5 text-xs font-semibold text-slate-300 hover:text-white"
              >
                Close
              </button>

              <button
                type="submit"
                disabled={!driveUrl.trim() || driveImporting}
                className="flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-xs font-semibold text-white hover:bg-emerald-500 disabled:opacity-50 transition-colors shadow-lg shadow-emerald-600/20"
              >
                {driveImporting ? (
                  <>
                    <div className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent" />
                    Downloading from Drive...
                  </>
                ) : (
                  <>
                    <FolderDown className="h-4 w-4" />
                    Import from Drive
                  </>
                )}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

