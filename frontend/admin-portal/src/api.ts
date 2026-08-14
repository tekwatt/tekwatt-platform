const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

export type TokenResponse = { accessToken: string; refreshToken: string; tokenType: string; expiresIn: number };
export type Tenant = { id: string; name: string; code?: string; status?: string };
export type Charger = { id: string; tenantId: string; stationId: string; serialNumber: string; vendor: string; model: string; protocolVersion: string; status: string; lastHeartbeat?: string };
export type ChargingSession = { id: string; transactionId: string; chargerId: string; status: string; energyKwh?: number; totalCost?: number; currency?: string; startedAt?: string; stoppedAt?: string };
export type Payment = { id: string; amount?: number; currency?: string; status?: string; createdAt?: string };
export type UserProfile = { id: string; firstName?: string; lastName?: string; email: string; status?: string };
export type Report = { id: string; type?: string; fileName?: string; status?: string; createdAt?: string };

class ApiError extends Error {
  constructor(message: string, public status: number) { super(message); }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = sessionStorage.getItem('tekwatt-access-token');
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try { const body = await response.json(); message = body.message || body.detail || message; } catch { /* non-JSON error */ }
    throw new ApiError(message, response.status);
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}

const pageContent = <T>(value: { content?: T[] } | T[]) => Array.isArray(value) ? value : value.content ?? [];

export const api = {
  login: (email: string, password: string) => request<TokenResponse>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (email: string, password: string) => request<TokenResponse>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  tenants: async () => pageContent(await request<{ content: Tenant[] }>('/api/v1/tenants?page=0&size=100')),
  chargers: (tenantId: string) => request<Charger[]>(`/api/v1/chargers?tenantId=${encodeURIComponent(tenantId)}`),
  sessions: (tenantId: string) => request<ChargingSession[]>(`/api/v1/charging-sessions?tenantId=${encodeURIComponent(tenantId)}`),
  payments: (tenantId: string) => request<Payment[]>(`/api/v1/payments?tenantId=${encodeURIComponent(tenantId)}`),
  users: async (tenantId: string) => pageContent(await request<{ content: UserProfile[] }>(`/api/v1/users?tenantId=${encodeURIComponent(tenantId)}&page=0&size=100`)),
  reports: async (tenantId: string) => pageContent(await request<{ content: Report[] }>(`/api/v1/reports?tenantId=${encodeURIComponent(tenantId)}&page=0&size=100`)),
  createCharger: (body: { tenantId: string; stationId: string; serialNumber: string; vendor: string; model: string; protocolVersion: string }) => request<Charger>('/api/v1/chargers', { method: 'POST', body: JSON.stringify(body) }),
};
