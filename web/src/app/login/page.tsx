"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Camera, Lock, Mail, User, Building, AlertCircle, Sparkles } from "lucide-react";
import { api, setToken } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [isRegister, setIsRegister] = useState(false);
  const [email, setEmail] = useState("demo@snapsync.io");
  const [password, setPassword] = useState("SnapSync2026!");
  const [fullName, setFullName] = useState("");
  const [companyName, setCompanyName] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (isRegister) {
        await api.register({
          email: email.trim(),
          password: password.trim(),
          full_name: fullName.trim(),
          company_name: companyName.trim() || undefined,
        });
      } else {
        await api.login(email.trim(), password.trim());
      }
      router.push("/dashboard");
    } catch (err: any) {
      // Graceful fallback to Demo Mode if cloud backend is unreachable
      if (err.message?.includes("fetch") || err.message?.includes("Network")) {
        setToken("demo_token_sync_ai_labs");
        router.push("/dashboard");
      } else {
        setError(err.message || "Authentication failed. Please check credentials.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDemoSignIn = () => {
    setToken("demo_token_sync_ai_labs");
    router.push("/dashboard");
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center p-4">
      <div className="w-full max-w-md rounded-2xl border border-dark-border bg-dark-surface p-8 shadow-2xl">
        <div className="text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-blue text-white shadow-lg shadow-brand-blue/20 mb-4">
            <Camera className="h-6 w-6" />
          </div>
          <h1 className="text-2xl font-bold text-white">
            {isRegister ? "Create Studio Account" : "Photographer Sign In"}
          </h1>
          <p className="mt-1 text-xs text-slate-400">
            {isRegister
              ? "Register to configure cameras and live events"
              : "Enter your studio credentials to access the Relay Console"}
          </p>
        </div>

        {error && (
          <div className="mt-6 flex items-center gap-2 rounded-xl bg-red-500/10 border border-red-500/30 p-3 text-xs text-red-300">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          {isRegister && (
            <>
              <div>
                <label className="text-xs font-medium text-slate-300">Full Name</label>
                <div className="relative mt-1">
                  <User className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
                  <input
                    type="text"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Alex Sterling"
                    className="w-full rounded-xl border border-dark-border bg-dark-card py-2.5 pl-10 pr-4 text-xs text-white placeholder-slate-500 focus:border-brand-blue focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-medium text-slate-300">Studio / Company Name</label>
                <div className="relative mt-1">
                  <Building className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
                  <input
                    type="text"
                    value={companyName}
                    onChange={(e) => setCompanyName(e.target.value)}
                    placeholder="Sterling Visuals Ltd"
                    className="w-full rounded-xl border border-dark-border bg-dark-card py-2.5 pl-10 pr-4 text-xs text-white placeholder-slate-500 focus:border-brand-blue focus:outline-none"
                  />
                </div>
              </div>
            </>
          )}

          <div>
            <label className="text-xs font-medium text-slate-300">Email Address</label>
            <div className="relative mt-1">
              <Mail className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="photographer@studio.com"
                className="w-full rounded-xl border border-dark-border bg-dark-card py-2.5 pl-10 pr-4 text-xs text-white placeholder-slate-500 focus:border-brand-blue focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-medium text-slate-300">Password</label>
            <div className="relative mt-1">
              <Lock className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full rounded-xl border border-dark-border bg-dark-card py-2.5 pl-10 pr-4 text-xs text-white placeholder-slate-500 focus:border-brand-blue focus:outline-none"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-xl bg-brand-blue py-3 text-xs font-semibold text-white shadow-lg shadow-brand-blue/20 hover:bg-brand-blueHover disabled:opacity-50 transition-all mt-2"
          >
            {loading ? "Authenticating..." : isRegister ? "Create Account" : "Sign In to Console"}
          </button>

          <button
            type="button"
            onClick={handleDemoSignIn}
            className="w-full rounded-xl border border-dark-border bg-dark-card py-2.5 text-xs font-semibold text-slate-300 hover:text-white hover:border-brand-blue transition-all flex items-center justify-center gap-2 mt-2"
          >
            <Sparkles className="h-3.5 w-3.5 text-brand-cyan" />
            Quick Demo Sign In (Explore Console)
          </button>
        </form>

        <div className="mt-6 text-center">
          <button
            onClick={() => {
              setIsRegister(!isRegister);
              setError(null);
            }}
            className="text-xs text-slate-400 hover:text-brand-blue transition-colors"
          >
            {isRegister
              ? "Already have an account? Sign in"
              : "Need a photographer account? Register here"}
          </button>
        </div>
      </div>
    </div>
  );
}

