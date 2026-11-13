"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Plus, Calendar, MapPin, QrCode, ExternalLink, Image as ImageIcon, Sparkles } from "lucide-react";
import { api } from "@/lib/api";
import QRCodeModal from "@/components/QRCodeModal";

export default function EventsPage() {
  const [events, setEvents] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedEventForQr, setSelectedEventForQr] = useState<any>(null);

  // Form state
  const [title, setTitle] = useState("");
  const [eventDate, setEventDate] = useState("2026-11-20");
  const [location, setLocation] = useState("");
  const [description, setDescription] = useState("");
  const [allowFaceSearch, setAllowFaceSearch] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const fetchEvents = async () => {
    try {
      const data = await api.getEvents().catch(() => [
        {
          id: "e4b52b21-4d32-443b-8198-44477c7fce8a",
          title: "Horizon Tech Gala 2026",
          event_code: "GALA-2026",
          event_date: "2026-11-20",
          location: "Metropolitan Grand Ballroom",
          status: "active",
          total_photos_count: 142,
          is_public: true,
          allow_face_search: true,
        },
        {
          id: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          title: "Sophia & Alexander Royal Wedding",
          event_code: "SOPHIA-ALEX-2026",
          event_date: "2026-11-25",
          location: "The Grand Estate, Napa Valley",
          status: "draft",
          total_photos_count: 0,
          is_public: true,
          allow_face_search: true,
        }
      ]);
      setEvents(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title) return;
    setSubmitting(true);
    try {
      await api.createEvent({
        title,
        event_date: eventDate,
        location: location || undefined,
        description: description || undefined,
        allow_face_search: allowFaceSearch,
        is_public: true,
      });
      setShowCreateModal(false);
      setTitle("");
      setLocation("");
      fetchEvents();
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white">Event Galleries</h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1">
            Create and manage live relay sessions with dedicated QR codes and AI face matching.
          </p>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 rounded-xl bg-brand-blue px-4 py-2.5 text-xs font-semibold text-white hover:bg-brand-blueHover transition-colors shadow-lg shadow-brand-blue/20"
        >
          <Plus className="h-4 w-4" />
          Create New Event
        </button>
      </div>

      {/* Events Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {events.map((event) => (
          <div
            key={event.id}
            className="flex flex-col justify-between rounded-2xl border border-dark-border bg-dark-surface p-6 shadow-xl hover:border-dark-border/80 transition-all"
          >
            <div>
              <div className="flex items-center justify-between gap-2">
                <span className="rounded-full bg-brand-blue/10 border border-brand-blue/20 px-2.5 py-0.5 text-[10px] font-bold text-brand-blue uppercase font-mono">
                  {event.event_code}
                </span>
                <span
                  className={`rounded-full px-2 py-0.5 text-[10px] font-bold uppercase ${
                    event.status === "active"
                      ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/30"
                      : "bg-slate-500/10 text-slate-400 border border-slate-500/30"
                  }`}
                >
                  {event.status}
                </span>
              </div>

              <h2 className="text-lg font-bold text-white mt-3 leading-snug">{event.title}</h2>

              <div className="mt-4 space-y-2 text-xs text-slate-400">
                <div className="flex items-center gap-2">
                  <Calendar className="h-3.5 w-3.5 text-slate-500" />
                  <span>{event.event_date}</span>
                </div>
                {event.location && (
                  <div className="flex items-center gap-2">
                    <MapPin className="h-3.5 w-3.5 text-slate-500" />
                    <span>{event.location}</span>
                  </div>
                )}
                <div className="flex items-center gap-2">
                  <ImageIcon className="h-3.5 w-3.5 text-slate-500" />
                  <span>{event.total_photos_count || 0} live photographs</span>
                </div>
              </div>
            </div>

            <div className="mt-6 pt-4 border-t border-dark-border flex items-center justify-between gap-2">
              <button
                onClick={() => setSelectedEventForQr(event)}
                className="flex items-center gap-1.5 rounded-lg border border-dark-border bg-dark-card px-3 py-2 text-xs font-semibold text-slate-300 hover:text-white hover:border-brand-blue transition-colors"
              >
                <QrCode className="h-3.5 w-3.5 text-brand-cyan" />
                QR Code
              </button>

              <Link
                href={`/gallery/${event.event_code}`}
                target="_blank"
                className="flex items-center gap-1.5 rounded-lg bg-brand-blue/10 border border-brand-blue/30 px-3 py-2 text-xs font-semibold text-brand-blue hover:bg-brand-blue hover:text-white transition-colors"
              >
                <ExternalLink className="h-3.5 w-3.5" />
                Open Live Wall
              </Link>
            </div>
          </div>
        ))}
      </div>

      {/* Create Event Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="relative w-full max-w-lg rounded-2xl border border-dark-border bg-dark-surface p-6 shadow-2xl">
            <h2 className="text-xl font-bold text-white">Create New Event</h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Generate unique event code and live gallery for attendee photo relay.
            </p>

            <form onSubmit={handleCreate} className="mt-6 space-y-4">
              <div>
                <label className="text-xs font-medium text-slate-300">Event Title *</label>
                <input
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. Vance & Sterling Royal Wedding"
                  className="mt-1 w-full rounded-xl border border-dark-border bg-dark-card py-2.5 px-4 text-xs text-white placeholder-slate-500 focus:border-brand-blue focus:outline-none"
                />
              </div>

              <div>
                <label className="text-xs font-medium text-slate-300">Event Date</label>
                <input
                  type="date"
                  required
                  value={eventDate}
                  onChange={(e) => setEventDate(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-dark-border bg-dark-card py-2.5 px-4 text-xs text-white focus:border-brand-blue focus:outline-none"
                />
              </div>

              <div>
                <label className="text-xs font-medium text-slate-300">Location / Venue</label>
                <input
                  type="text"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  placeholder="The Grand Estate Ballroom"
                  className="mt-1 w-full rounded-xl border border-dark-border bg-dark-card py-2.5 px-4 text-xs text-white placeholder-slate-500 focus:border-brand-blue focus:outline-none"
                />
              </div>

              <div>
                <label className="text-xs font-medium text-slate-300">Description</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Notes about ceremony, lighting, coverage schedule..."
                  className="mt-1 w-full rounded-xl border border-dark-border bg-dark-card py-2.5 px-4 text-xs text-white placeholder-slate-500 focus:border-brand-blue focus:outline-none h-20"
                />
              </div>

              <div className="flex items-center justify-between rounded-xl border border-dark-border bg-dark-card p-3">
                <div className="flex items-center gap-2">
                  <Sparkles className="h-4 w-4 text-brand-cyan" />
                  <div>
                    <p className="text-xs font-semibold text-white">Enable AI Face Matching</p>
                    <p className="text-[10px] text-slate-400">Attendees can scan QR and find photos by selfie</p>
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={allowFaceSearch}
                  onChange={(e) => setAllowFaceSearch(e.target.checked)}
                  className="h-4 w-4 rounded border-dark-border bg-dark-bg text-brand-blue focus:ring-brand-blue"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-dark-border">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="rounded-xl border border-dark-border px-4 py-2.5 text-xs font-semibold text-slate-300 hover:text-white"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="rounded-xl bg-brand-blue px-5 py-2.5 text-xs font-semibold text-white hover:bg-brand-blueHover disabled:opacity-50"
                >
                  {submitting ? "Creating..." : "Create Event"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* QR Modal */}
      {selectedEventForQr && (
        <QRCodeModal
          event={selectedEventForQr}
          isOpen={!!selectedEventForQr}
          onClose={() => setSelectedEventForQr(null)}
        />
      )}
    </div>
  );
}

