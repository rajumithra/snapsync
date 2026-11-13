"use client";

import { useEffect, useRef, useState } from "react";
import { X, QrCode, Copy, Check, Download, ExternalLink, RefreshCw } from "lucide-react";
import QRCode from "qrcode";

interface QRCodeModalProps {
  event: any;
  isOpen: boolean;
  onClose: () => void;
}

export default function QRCodeModal({ event, isOpen, onClose }: QRCodeModalProps) {
  const [copied, setCopied] = useState(false);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  const publicUrl = typeof window !== "undefined" && event
    ? `${window.location.origin}/gallery/${event.event_code || event.id}`
    : `/gallery/${event?.event_code || event?.id}`;

  useEffect(() => {
    if (!isOpen || !event) return;

    // Generate crisp QR code on Canvas
    if (canvasRef.current) {
      QRCode.toCanvas(
        canvasRef.current,
        publicUrl,
        {
          width: 240,
          margin: 2,
          color: {
            dark: "#0F172A",
            light: "#FFFFFF",
          },
        },
        (error) => {
          if (!error && canvasRef.current) {
            setQrDataUrl(canvasRef.current.toDataURL("image/png"));
          }
        }
      );
    }
  }, [isOpen, event, publicUrl]);

  if (!isOpen || !event) return null;

  const copyUrl = () => {
    navigator.clipboard.writeText(publicUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const downloadQr = () => {
    if (qrDataUrl) {
      const a = document.createElement("a");
      a.href = qrDataUrl;
      a.download = `QR_${event.event_code || "event"}.png`;
      a.click();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-sm p-4">
      <div className="relative w-full max-w-md rounded-2xl border border-dark-border bg-dark-surface p-6 shadow-2xl text-center">
        <button
          onClick={onClose}
          className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-dark-card hover:text-white transition-colors"
        >
          <X className="h-5 w-5" />
        </button>

        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-blue/20 text-brand-blue mb-3">
          <QrCode className="h-6 w-6" />
        </div>

        <h3 className="text-xl font-bold text-white">{event.title}</h3>
        <p className="text-xs text-brand-cyan font-mono mt-0.5">Code: {event.event_code}</p>
        <p className="mt-2 text-xs text-slate-400">
          Guests scan this QR code with their mobile cameras to view live photos and use AI face matching.
        </p>

        {/* QR Code Canvas */}
        <div className="my-6 flex flex-col items-center justify-center">
          <div className="rounded-2xl bg-white p-4 shadow-xl border border-slate-200">
            <canvas ref={canvasRef} className="rounded-lg" />
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col gap-2.5">
          <div className="flex items-center gap-2">
            <button
              onClick={copyUrl}
              className="flex-1 flex items-center justify-center gap-2 rounded-xl border border-dark-border bg-dark-card py-2.5 px-4 text-xs font-semibold text-white hover:border-brand-blue transition-colors"
            >
              {copied ? <Check className="h-4 w-4 text-emerald-400" /> : <Copy className="h-4 w-4 text-slate-400" />}
              {copied ? "Copied!" : "Copy Link"}
            </button>

            <button
              onClick={downloadQr}
              className="flex-1 flex items-center justify-center gap-2 rounded-xl border border-dark-border bg-dark-card py-2.5 px-4 text-xs font-semibold text-white hover:border-brand-blue transition-colors"
            >
              <Download className="h-4 w-4 text-brand-cyan" />
              Download QR
            </button>
          </div>

          <a
            href={`/gallery/${event.event_code || event.id}`}
            target="_blank"
            rel="noreferrer"
            className="flex items-center justify-center gap-2 rounded-xl bg-brand-blue py-2.5 px-4 text-xs font-semibold text-white hover:bg-brand-blueHover transition-colors shadow-lg shadow-brand-blue/20"
          >
            <ExternalLink className="h-4 w-4" />
            Open Guest Portal
          </a>
        </div>
      </div>
    </div>
  );
}
