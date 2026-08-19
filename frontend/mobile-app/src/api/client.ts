import type { Charger, ChargingSession, Connector, Payment, RegistrationInput, SupportTicket, Tenant, TokenResponse, UserProfile, Wallet } from '../types';

export const API_BASE_URL = (process.env.EXPO_PUBLIC_API_BASE_URL || 'http://10.0.2.2:8080').replace(/\/$/, '');

const messages: Record<number, string> = {
  400: 'Some information is invalid. Please review it and try again.',
  401: 'Your sign-in has expired. Please sign in again.',
  403: 'You do not have access to this action.',
  404: 'The requested information could not be found.',
  408: 'The request took too long. Check your connection and try again.',
  409: 'This action conflicts with the current record. Refresh and try again.',
  422: 'Some information could not be accepted. Please check the form.',
  500: 'Something went wrong while processing your request.',
  502: 'A required TekWatt service is not responding correctly.',
  503: 'This TekWatt service is temporarily unavailable.',
  504: 'A required service took too long to respond.',
};

export class ApiError extends Error {
  constructor(message: string, readonly status: number) { super(message); }
}

type ApiOptions = RequestInit & { token?: string };

async function request<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 25_000);
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
        ...options.headers,
      },
    });
    if (!response.ok) {
      let detail = '';
      try {
        const body = await response.json() as { message?: string; detail?: string; error?: string };
        detail = body.detail || body.message || body.error || '';
      } catch { /* no readable response body */ }
      if (response.status === 401 && path === '/api/v1/auth/login') detail = 'The email address or password is incorrect.';
      throw new ApiError(detail || messages[response.status] || 'The request could not be completed.', response.status);
    }
    return response.status === 204 ? undefined as T : response.json() as Promise<T>;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (error instanceof Error && error.name === 'AbortError') throw new ApiError(messages[408]!, 408);
    throw new ApiError(`Cannot reach TekWatt at ${API_BASE_URL}. Check that the backend is running and the mobile API URL is correct.`, 0);
  } finally {
    clearTimeout(timeout);
  }
}

const pageContent = <T>(value: { content?: T[] } | T[]) => Array.isArray(value) ? value : value.content || [];

export const api = {
  login: (email: string, password: string) => request<TokenResponse>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  registerAuth: (email: string, password: string) => request<TokenResponse>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  logout: (refreshToken: string) => request<void>('/api/v1/auth/logout', { method: 'POST', body: JSON.stringify({ refreshToken }) }),
  tenants: async (token: string) => pageContent(await request<{ content: Tenant[] }>('/api/v1/tenants?page=0&size=100', { token })),
  userByAuth: (authUserId: string, token: string) => request<UserProfile>(`/api/v1/users/by-auth-user/${authUserId}`, { token }),
  createUser: (authUserId: string, tenantId: string, input: RegistrationInput, token: string) => request<UserProfile>('/api/v1/users', { method: 'POST', token, body: JSON.stringify({ authUserId, tenantId, firstName: input.firstName, lastName: input.lastName, fullName: `${input.firstName} ${input.lastName}`.trim(), email: input.email, phone: input.phone || undefined, status: 'ACTIVE' }) }),
  chargers: (tenantId: string, token: string) => request<Charger[]>(`/api/v1/chargers?tenantId=${encodeURIComponent(tenantId)}`, { token }),
  connectors: (chargerId: string, token: string) => request<Connector[]>(`/api/v1/connectors?chargerId=${encodeURIComponent(chargerId)}`, { token }),
  sessions: (tenantId: string, token: string) => request<ChargingSession[]>(`/api/v1/charging-sessions?tenantId=${encodeURIComponent(tenantId)}`, { token }),
  startSession: (body: Record<string, unknown>, token: string) => request<ChargingSession>('/api/v1/charging-sessions', { method: 'POST', token, body: JSON.stringify(body) }),
  stopSession: (id: string, meterStopWh: number, token: string) => request<ChargingSession>(`/api/v1/charging-sessions/${id}/stop`, { method: 'POST', token, body: JSON.stringify({ meterStopWh, status: 'COMPLETED' }) }),
  wallets: (tenantId: string, token: string) => request<Wallet[]>(`/api/v1/payments/operations/wallets?tenantId=${encodeURIComponent(tenantId)}`, { token }),
  createWallet: (tenantId: string, userId: string, token: string) => request<Wallet>('/api/v1/payments/operations/wallets', { method: 'POST', token, body: JSON.stringify({ tenantId, userId, currency: 'INR' }) }),
  payments: (tenantId: string, token: string) => request<Payment[]>(`/api/v1/payments?tenantId=${encodeURIComponent(tenantId)}`, { token }),
  tickets: (tenantId: string, token: string) => request<SupportTicket[]>(`/api/v1/support/tickets?tenantId=${encodeURIComponent(tenantId)}`, { token }),
  createTicket: (body: Record<string, unknown>, token: string) => request<SupportTicket>('/api/v1/support/tickets', { method: 'POST', token, body: JSON.stringify(body) }),
};
