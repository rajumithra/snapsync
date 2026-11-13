"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Camera, Calendar, LayoutDashboard, Shield, LogOut } from "lucide-react";
import { clearToken, getToken } from "@/lib/api";
import { useEffect, useState } from "react";

export default function Navbar() {
  const pathname = usePathname();
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    setIsLoggedIn(!!getToken());
  }, [pathname]);

  const navLinks = [
    { href: "/dashboard", label: "Relay Console", icon: LayoutDashboard },
    { href: "/events", label: "Events", icon: Calendar },
    { href: "/admin", label: "Admin", icon: Shield },
  ];

  return (
    <header className="sticky top-0 z-40 w-full border-b border-dark-border bg-dark-bg/95 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        {/* Brand */}
        <Link href="/" className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-blue text-white shadow-lg shadow-brand-blue/20">
            <Camera className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="text-lg font-bold tracking-tight text-white">SnapSync</span>
              <span className="hidden sm:inline-block ml-1 rounded-full bg-brand-blue/10 px-2 py-0.5 text-[10px] font-semibold text-brand-blue uppercase tracking-wider">
                Live Relay
              </span>
            </div>
            <p className="text-[10px] text-slate-400 font-medium tracking-tight">by Sync AI Labs</p>
          </div>
        </Link>

        {/* Navigation links */}
        <nav className="flex items-center gap-1 sm:gap-2">
          {navLinks.map((link) => {
            const Icon = link.icon;
            const isActive = pathname.startsWith(link.href);
            return (
              <Link
                key={link.href}
                href={link.href}
                className={`flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs sm:text-sm font-medium transition-colors ${
                  isActive
                    ? "bg-dark-surface text-brand-blue border border-dark-border"
                    : "text-slate-400 hover:bg-dark-surface/60 hover:text-white"
                }`}
              >
                <Icon className="h-4 w-4" />
                <span>{link.label}</span>
              </Link>
            );
          })}

          {/* Auth Action */}
          {isLoggedIn ? (
            <button
              onClick={() => {
                clearToken();
                window.location.href = "/login";
              }}
              className="ml-2 flex items-center gap-1.5 rounded-lg border border-dark-border px-3 py-2 text-xs sm:text-sm text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors"
              title="Sign Out"
            >
              <LogOut className="h-4 w-4" />
              <span className="hidden sm:inline">Logout</span>
            </button>
          ) : (
            <Link
              href="/login"
              className="ml-2 rounded-lg bg-brand-blue px-4 py-2 text-xs sm:text-sm font-semibold text-white hover:bg-brand-blueHover transition-colors shadow-sm"
            >
              Sign In
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
}

