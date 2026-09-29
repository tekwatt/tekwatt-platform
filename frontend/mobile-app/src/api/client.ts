import type { Bill, Charger, ChargingSession, Connector, CpoIdentity, CpoOverview, Invoice, Payment, RazorpayOrder, RegistrationInput, Reservation, RfidCard, SupportTicket, Tenant, TicketComment, TicketDetail, TokenResponse, UserProfile, UserSession, Wallet, WalletEntry } from '../types';

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


export const api = {
  login: (email: string, password: string) => request<TokenResponse>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  requestPasswordReset: (email: string) => request<void>('/api/v1/auth/password-reset/request', { method: 'POST', body: JSON.stringify({ email }) }),
  confirmPasswordReset: (email: string, code: string, newPassword: string) => request<void>('/api/v1/auth/password-reset/confirm', { method: 'POST', body: JSON.stringify({ email, code, newPassword }) }),
  registerAuth: (email: string, password: string) => request<TokenResponse>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  refreshAuth: (refreshToken: string) => request<TokenResponse>('/api/v1/auth/refresh', { method: 'POST', body: JSON.stringify({ refreshToken }) }),
  logout: (refreshToken: string, token: string) => request<void>('/api/v1/auth/logout', { method: 'POST', token, body: JSON.stringify({ refreshToken }) }),
  authSessions: (token:string)=>request<UserSession[]>('/api/v1/auth/sessions',{token}),
  revokeSession: (id:string,token:string)=>request<void>(`/api/v1/auth/sessions/${id}`,{method:'DELETE',token}),
  revokeOtherSessions: (token:string)=>request<void>('/api/v1/auth/sessions/revoke-others',{method:'POST',token}),
  tenant: (id: string, token: string) => request<Tenant>(`/api/v1/tenants/${encodeURIComponent(id)}`, { token }),
  myProfile: (token: string) => request<UserProfile>('/api/v1/users/me', { token }),
  cpoIdentity: (token: string) => request<CpoIdentity>('/api/v1/users/cpo/me', { token }),
  cpoOverview: (token: string) => request<CpoOverview>('/api/v1/users/cpo/me/overview', { token }),
  createUser: (authUserId: string, tenantId: string, input: RegistrationInput, token: string) => request<UserProfile>('/api/v1/users', { method: 'POST', token, body: JSON.stringify({ authUserId, tenantId, firstName: input.firstName, lastName: input.lastName, fullName: `${input.firstName} ${input.lastName}`.trim(), email: input.email, phone: input.phone || undefined, status: 'ACTIVE' }) }),
  updateMyProfile: (body:{firstName:string;lastName:string;phone:string;city:string;zipcode:string},token:string)=>request<UserProfile>('/api/v1/users/me',{method:'PUT',token,body:JSON.stringify(body)}),
  myRfidCards: (token:string)=>request<RfidCard[]>('/api/v1/users/me/rfid-cards',{token}),
  myChargers: (token: string) => request<Charger[]>('/api/v1/chargers/my', { token }),
  myConnectors: (chargerId: string, token: string) => request<Connector[]>(`/api/v1/connectors/my?chargerId=${encodeURIComponent(chargerId)}`, { token }),
  // The backend derives the workspace and customer from the active login.
  sessions: (_tenantId: string, token: string) => request<ChargingSession[]>('/api/v1/charging-sessions/my', { token }),
  remoteStartForMe: (chargerId:string,connectorId:string,token:string) => request<{messageId:string}>('/api/v1/ocpp/customer-commands/start', { method:'POST', token, body:JSON.stringify({chargerId,connectorId}) }),
  remoteStopForMe: (sessionId:string,token:string) => request<{messageId:string}>('/api/v1/ocpp/customer-commands/stop', { method:'POST', token, body:JSON.stringify({sessionId}) }),
  ocppCommandResult: (messageId: string, token: string) => request<{result:string;message?:string}>(`/api/v1/ocpp/customer-commands/${encodeURIComponent(messageId)}/result`, { token }),
  myReservations: (token:string)=>request<Reservation[]>('/api/v1/reservations/my',{token}),
  createMyReservation: (body:{chargerId:string;connectorId:string;startsAt:string;expiresAt:string},token:string)=>request<Reservation>('/api/v1/reservations/my',{method:'POST',token,body:JSON.stringify(body)}),
  cancelMyReservation: (id:string,token:string)=>request<Reservation>(`/api/v1/reservations/my/${encodeURIComponent(id)}/cancel`,{method:'POST',token}),
  completeMyReservation: (id:string,token:string)=>request<Reservation>(`/api/v1/reservations/my/${encodeURIComponent(id)}/complete`,{method:'POST',token}),
  myWallet: (token: string) => request<Wallet | null>('/api/v1/payments/my/wallet', { token }),
  createMyWallet: (token: string) => request<Wallet>('/api/v1/payments/my/wallet', { method: 'POST', token }),
  myWalletEntries: (token:string)=>request<WalletEntry[]>('/api/v1/payments/my/wallet/entries',{token}),
  myPayments: (token:string)=>request<Payment[]>('/api/v1/payments/my',{token}),
  myInvoices: (token:string)=>request<Invoice[]>('/api/v1/invoices/my',{token}),
  myRazorpayOrder: (invoiceId:string,token:string)=>request<RazorpayOrder>(`/api/v1/payments/my/invoices/${encodeURIComponent(invoiceId)}/razorpay-order`,{method:'POST',token}),
  verifyMyRazorpayPayment: (body:{paymentId:string;razorpayPaymentId:string;razorpayOrderId:string;razorpaySignature:string},token:string)=>request<Payment>('/api/v1/payments/my/razorpay-verify',{method:'POST',token,body:JSON.stringify(body)}),
  settleMyInvoice: (invoiceId:string,token:string)=>request<Invoice>(`/api/v1/payments/my/invoices/${encodeURIComponent(invoiceId)}/settle`,{method:'POST',token}),
  myBill: (id:string,token:string)=>request<Bill>(`/api/v1/bills/my/${encodeURIComponent(id)}`,{token}),
  session: (id:string,token:string)=>request<ChargingSession>(`/api/v1/charging-sessions/${encodeURIComponent(id)}`,{token}),
  myTickets: (token: string) => request<SupportTicket[]>('/api/v1/support/tickets/my', { token }),
  myTicket: (id:string,token:string)=>request<TicketDetail>(`/api/v1/support/tickets/my/${encodeURIComponent(id)}`,{token}),
  createMyTicket: (body: {subject:string;description:string;category:string;priority:string}, token: string) => request<SupportTicket>('/api/v1/support/tickets/my', { method: 'POST', token, body: JSON.stringify(body) }),
  addMyTicketComment: (id:string,body:string,token:string)=>request<TicketComment>(`/api/v1/support/tickets/my/${encodeURIComponent(id)}/comments`,{method:'POST',token,body:JSON.stringify({body})}),
};
