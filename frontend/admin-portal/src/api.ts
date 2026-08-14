const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

export type TokenResponse = { accessToken: string; refreshToken: string; tokenType: string; expiresIn: number };
export type Tenant = { id: string; name: string; code?: string; status?: string };
export type Charger = { id: string; tenantId: string; stationId: string; serialNumber: string; vendor: string; model: string; protocolVersion: string; status: string; lastHeartbeat?: string };
export type ChargingSession = { id: string; transactionId: string; chargerId: string; status: string; energyKwh?: number; totalCost?: number; currency?: string; startedAt?: string; stoppedAt?: string };
export type Payment = { id: string; amount?: number; currency?: string; status?: string; createdAt?: string };
export type UserProfile = { id: string; firstName?: string; lastName?: string; email: string; status?: string };
export type Report = { id: string; type?: string; fileName?: string; status?: string; createdAt?: string };
export type Notification = { id: string; channel: string; recipient: string; subject?: string; body: string; status: string; createdAt?: string };

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
  refundPayment: (id: string) => request<Payment>(`/api/v1/payments/${id}/refund`, { method: 'POST' }),
  users: async (tenantId: string) => pageContent(await request<{ content: UserProfile[] }>(`/api/v1/users?tenantId=${encodeURIComponent(tenantId)}&page=0&size=100`)),
  createUser: (body: { authUserId: string; tenantId: string; firstName: string; lastName: string; email: string; phone?: string }) => request<UserProfile>('/api/v1/users', { method: 'POST', body: JSON.stringify(body) }),
  deactivateUser: (id: string) => request<void>(`/api/v1/users/${id}`, { method: 'DELETE' }),
  reports: async (tenantId: string) => pageContent(await request<{ content: Report[] }>(`/api/v1/reports?tenantId=${encodeURIComponent(tenantId)}&page=0&size=100`)),
  createReport: (body: { tenantId: string; reportType: string; format: string; from: string; to: string }) => request<Report>('/api/v1/reports', { method: 'POST', body: JSON.stringify(body) }),
  downloadReport: async (id: string, fileName = 'report') => {
    const token = sessionStorage.getItem('tekwatt-access-token');
    const response = await fetch(`${API_BASE}/api/v1/reports/${id}/download`, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
    if (!response.ok) throw new ApiError(`Report download failed (${response.status})`, response.status);
    const url = URL.createObjectURL(await response.blob()); const link = document.createElement('a'); link.href = url; link.download = fileName; link.click(); URL.revokeObjectURL(url);
  },
  notifications: (tenantId: string) => request<Notification[]>(`/api/v1/notifications?tenantId=${encodeURIComponent(tenantId)}`),
  createNotification: (body: { tenantId: string; idempotencyKey: string; channel: string; recipient: string; subject?: string; body: string; maxAttempts: number }) => request<Notification>('/api/v1/notifications', { method: 'POST', body: JSON.stringify(body) }),
  sendNotification: (id: string) => request<Notification>(`/api/v1/notifications/${id}/send`, { method: 'POST' }),
  updateTenant: (id: string, body: { name: string; slug: string; contactEmail: string }) => request<Tenant>(`/api/v1/tenants/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  createCharger: (body: { tenantId: string; stationId: string; serialNumber: string; vendor: string; model: string; protocolVersion: string }) => request<Charger>('/api/v1/chargers', { method: 'POST', body: JSON.stringify(body) }),
};
