const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "";

export async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json", ...(init?.headers ?? {}) },
    ...init
  });
  if (!response.ok) {
    const message = await response.text();
    throw new Error(`${response.status} ${response.statusText}: ${message}`);
  }
  return (await response.json()) as T;
}

export type ModeSubscriptions = {
  paper: string[];
  live: string[];
};

export function normalizeByModes(data: Record<string, string[]> | null | undefined): ModeSubscriptions {
  if (!data) {
    return { paper: [], live: [] };
  }
  return { paper: data.paper ?? [], live: data.live ?? [] };
}
