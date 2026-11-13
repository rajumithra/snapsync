import Link from "next/link";
import { Camera, Wifi, Cloud, Sparkles, QrCode, ShieldCheck, ArrowRight, Zap, CheckCircle2 } from "lucide-react";

export default function HomePage() {
  return (
    <div className="relative overflow-hidden">
      {/* Background Glow */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 h-96 w-96 rounded-full bg-brand-blue/15 blur-3xl -z-10 pointer-events-none" />

      {/* Hero Section */}
      <section className="mx-auto max-w-7xl px-4 pt-16 pb-20 sm:px-6 lg:px-8 text-center">
        <div className="inline-flex items-center gap-2 rounded-full border border-dark-border bg-dark-surface px-4 py-1.5 text-xs text-brand-cyan mb-8">
          <span className="flex h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
          <span>Real-Time Camera-to-Cloud Ingestion Engine</span>
        </div>

        <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-white max-w-4xl mx-auto leading-tight">
          Live Photo Relay from <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-cyan-400 to-indigo-400">Shutter to Cloud</span> in Seconds.
        </h1>

        <p className="mt-6 text-base sm:text-lg text-slate-400 max-w-2xl mx-auto leading-relaxed">
          Engineered for wedding photographers, corporate galas, and live media production. Connect Sony, Canon, and Nikon cameras via embedded Wi-Fi FTP, stream to high-speed cloud storage, and deliver instant AI face-matched galleries to attendees.
        </p>

        {/* CTA Buttons */}
        <div className="mt-10 flex flex-wrap items-center justify-center gap-4">
          <Link
            href="/dashboard"
            className="flex items-center gap-2 rounded-xl bg-brand-blue px-6 py-3.5 text-sm font-semibold text-white shadow-lg shadow-brand-blue/25 hover:bg-brand-blueHover transition-all"
          >
            Launch Relay Console
            <ArrowRight className="h-4 w-4" />
          </Link>

          <Link
            href="/events"
            className="flex items-center gap-2 rounded-xl border border-dark-border bg-dark-surface px-6 py-3.5 text-sm font-semibold text-slate-200 hover:border-brand-blue hover:text-white transition-all"
          >
            <Sparkles className="h-4 w-4 text-brand-cyan" />
            Explore Live Event Galleries
          </Link>
        </div>

        {/* Live Architecture Flow */}
        <div className="mt-20 rounded-2xl border border-dark-border bg-dark-surface/50 p-6 sm:p-10 text-left">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-brand-cyan mb-6 text-center">
            Zero-Intervention Live Relay Pipeline
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="rounded-xl border border-dark-border bg-dark-card p-5">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-500/10 text-brand-blue mb-4">
                <Camera className="h-5 w-5" />
              </div>
              <h3 className="text-sm font-bold text-white">1. Camera Shutter Press</h3>
              <p className="mt-1 text-xs text-slate-400 leading-normal">
                DSLR / Mirrorless camera captures high-res JPEG/RAW and streams instantly via Wi-Fi FTP.
              </p>
            </div>

            <div className="rounded-xl border border-dark-border bg-dark-card p-5">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-cyan-500/10 text-brand-cyan mb-4">
                <Wifi className="h-5 w-5" />
              </div>
              <h3 className="text-sm font-bold text-white">2. Android Edge Receiver</h3>
              <p className="mt-1 text-xs text-slate-400 leading-normal">
                Embedded FTP daemon validates binary hash, parses EXIF metadata, and generates WebP previews.
              </p>
            </div>

            <div className="rounded-xl border border-dark-border bg-dark-card p-5">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-indigo-500/10 text-indigo-400 mb-4">
                <Cloud className="h-5 w-5" />
              </div>
              <h3 className="text-sm font-bold text-white">3. Direct S3/R2 Cloud Sync</h3>
              <p className="mt-1 text-xs text-slate-400 leading-normal">
                Pre-signed signed URLs transfer original photos directly to edge cloud storage with 100% data preservation.
              </p>
            </div>

            <div className="rounded-xl border border-dark-border bg-dark-card p-5">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-emerald-500/10 text-emerald-400 mb-4">
                <Sparkles className="h-5 w-5" />
              </div>
              <h3 className="text-sm font-bold text-white">4. Guest Face Matching</h3>
              <p className="mt-1 text-xs text-slate-400 leading-normal">
                Guests scan the event QR code, take a selfie, and instantly receive photos matching their facial embeddings.
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

