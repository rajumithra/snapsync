export function getApiBaseUrl(): string {
  if (process.env.NEXT_PUBLIC_API_URL) return process.env.NEXT_PUBLIC_API_URL;
  if (typeof window !== "undefined") {
    return `${window.location.protocol}//${window.location.hostname}:8000/api/v1`;
  }
  return "http://localhost:8000/api/v1";
}

export function getWsBaseUrl(): string {
  if (process.env.NEXT_PUBLIC_WS_URL) return process.env.NEXT_PUBLIC_WS_URL;
  if (typeof window !== "undefined") {
    const wsProto = window.location.protocol === "https:" ? "wss:" : "ws:";
    return `${wsProto}//${window.location.hostname}:8000/ws`;
  }
  return "ws://localhost:8000/ws";
}

export const API_BASE_URL = getApiBaseUrl();
export const WS_BASE_URL = getWsBaseUrl();

export function formatImageUrl(url?: string | null): string {
  if (!url) return "";
  if (typeof window !== "undefined") {
    const host = window.location.hostname;
    if (host && host !== "localhost" && host !== "127.0.0.1") {
      return url.replace(/http:\/\/(localhost|127\.0\.0\.1):8000/, `${window.location.protocol}//${host}:8000`);
    }
  }
  return url;
}

export function getToken(): string | null {
  if (typeof window !== "undefined") {
    return localStorage.getItem("snapsync_token");
  }
  return null;
}

export function setToken(token: string) {
  if (typeof window !== "undefined") {
    localStorage.setItem("snapsync_token", token);
  }
}

export function clearToken() {
  if (typeof window !== "undefined") {
    localStorage.removeItem("snapsync_token");
  }
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(options.headers || {});

  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  if (!(options.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const url = `${getApiBaseUrl()}${endpoint}`;
  const response = await fetch(url, { ...options, headers });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.detail || `Request failed with status ${response.status}`);
  }

  return response.json();
}

export const api = {
  // Auth
  login: async (username: string, password: string) => {
    const formData = new URLSearchParams();
    formData.append("username", username);
    formData.append("password", password);
    const data = await request<{ access_token: string; user: any }>("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: formData.toString(),
    });
    setToken(data.access_token);
    return data;
  },

  register: async (payload: { email: string; password: string; full_name: string; company_name?: string }) => {
    const data = await request<{ access_token: string; user: any }>("/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    });
    setToken(data.access_token);
    return data;
  },

  getMe: () => request<any>("/auth/me"),

  // Events
  getEvents: () => request<any[]>("/events"),
  getEvent: (idOrCode: string) => request<any>(`/events/${idOrCode}`),
  createEvent: (payload: any) =>
    request<any>("/events", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  updateEventStatus: (id: string, status: string) =>
    request<any>(`/events/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    }),

  // Photos
  getEventPhotos: (eventIdOrCode: string, skip = 0, limit = 50) =>
    request<any[]>(`/photos/event/${eventIdOrCode}?skip=${skip}&limit=${limit}`),

  manualUploadPhotos: async (eventIdOrCode: string, files: File[]) => {
    const formData = new FormData();
    formData.append("event_id_or_code", eventIdOrCode);
    files.forEach((file) => {
      formData.append("files", file);
    });

    return request<any[]>("/photos/manual-upload", {
      method: "POST",
      body: formData,
    });
  },

  importPhotosFromDrive: async (eventIdOrCode: string, driveUrl: string, accessToken?: string) => {
    const formData = new FormData();
    formData.append("event_id_or_code", eventIdOrCode);
    formData.append("drive_url", driveUrl);
    if (accessToken) {
      formData.append("access_token", accessToken);
    }

    return request<any[]>("/photos/import-drive", {
      method: "POST",
      body: formData,
    });
  },

  // Face Recognition & Matching
  searchPhotosByFace: async (eventIdOrCode: string, selfieFile: File, minSimilarity = 0.60) => {
    const formData = new FormData();
    formData.append("event_id_or_code", eventIdOrCode);
    formData.append("min_similarity", minSimilarity.toString());
    formData.append("selfie", selfieFile);

    return request<{ match_count: number; matches: any[] }>("/face/search", {
      method: "POST",
      body: formData,
    });
  },

  // Telemetry & Stats
  getDashboardStats: () => request<any>("/admin/dashboard-stats"),
  getAdminOverview: () => request<any>("/admin/overview"),
};

