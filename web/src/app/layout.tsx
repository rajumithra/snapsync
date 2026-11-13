import "./globals.css";
import type { Metadata } from "next";
import Navbar from "@/components/Navbar";
import Link from "next/link";
import { Sparkles, ExternalLink, Shield, Terminal } from "lucide-react";

export const metadata: Metadata = {
  title: "SnapSync by Sync AI Labs – Live Photo Relay & Instant AI Distribution",
  description: "Enterprise camera-to-cloud photo relay engine by Sync AI Labs. Real-time Wi-Fi FTP ingestion, high-speed S3/R2 storage, and instant facial recognition galleries.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-dark-bg text-slate-100 flex flex-col">
        <Navbar />
        <main className="flex-1">{children}</main>
        
        {/* Sync AI Labs Footer */}
        <footer className="border-t border-dark-border bg-dark-surface/50 py-8 text-xs text-slate-400">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <span className="flex h-2 w-2 rounded-full bg-cyan-400" />
              <span>Developed & Deployed by</span>
              <a 
                href="https://syncailabs.in" 
                target="_blank" 
                rel="noreferrer"
                className="font-semibold text-white hover:text-brand-cyan transition-colors inline-flex items-center gap-1"
              >
                Sync AI Labs
                <ExternalLink className="h-3 w-3" />
              </a>
            </div>
            
            <div className="flex items-center gap-6 text-slate-400">
              <Link href="/dashboard" className="hover:text-white transition-colors">Relay Console</Link>
              <Link href="/gallery/GALA-2026" className="hover:text-white transition-colors">Live Gallery</Link>
              <Link href="/admin" className="hover:text-white transition-colors">Admin Telemetry</Link>
              <a href="http://localhost:8000/docs" target="_blank" rel="noreferrer" className="hover:text-white transition-colors">API Docs</a>
            </div>
            
            <div className="text-slate-400">
              © {new Date().getFullYear()} Sync AI Labs. All rights reserved.
            </div>
          </div>
        </footer>
      </body>
    </html>
  );
}

