const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

export type TokenResponse = { accessToken: string; refreshToken: string; tokenType: string; expiresIn: number };
export type Tenant = { id: string; name: string; code?: string; status?: string };
export type Charger = { id: string; tenantId: string; stationId: string; serialNumber: string; vendor: string; model: string; protocolVersion: string; status: string; lastHeartbeat?: string };
export type ChargingSession = { id: string; transactionId: string; chargerId: string; status: string; energyKwh?: number; totalCost?: number; currency?: string; startedAt?: string; stoppedAt?: string };
export type Payment = { id: string; amount?: number; currency?: string; status?: string; createdAt?: string };
export type UserProfile = { id: string; firstName?: string; lastName?: string; email: string; status?: string };
export type Report = { id: string; type?: string; fileName?: string; status?: string; createdAt?: string };
export type Notification = { id: string; channel: string; recipient: string; subject?: string; body: string; status: string; createdAt?: string };
export type Connector = { id: string; tenantId: string; chargerId: string; connectorNumber: number; type: string; maxPowerKw: number; maxVoltage: number; maxCurrent: number; status: string };
export type OcppConnection = { stationId: string; connected: boolean; connectedAt?: string; protocol?: string };
export type OcppMessage = { id: string; stationId: string; direction: string; messageType: number; uniqueId: string; action?: string; payload: string; createdAt: string };
export type Telemetry = { id: string; chargerId: string; connectorId: string; transactionId?: string; measurand: string; value: number; unit: string; sampledAt: string };
export type FirmwarePackage = { id: string; vendor: string; model: string; version: string; downloadUrl: string; active: boolean };
export type FirmwareJob = { id: string; tenantId: string; chargerId: string; firmwarePackageId: string; ocppVersion: string; status: string; scheduledAt: string; statusInfo?: string };
export type SupportTicket = { id:string; ticketNumber:string; tenantId:string; requesterId?:string; requesterName:string; requesterEmail:string; subject:string; description:string; category:string; priority:'LOW'|'MEDIUM'|'HIGH'|'URGENT'; status:'OPEN'|'ASSIGNED'|'IN_PROGRESS'|'WAITING_CUSTOMER'|'RESOLVED'|'CLOSED'|'CANCELLED'; assigneeId?:string; assigneeName?:string; stationId?:string; sessionId?:string; slaDueAt:string; firstResponseAt?:string; resolvedAt?:string; closedAt?:string; createdAt:string; updatedAt:string };
export type TicketComment = { id:string; ticketId:string; authorId?:string; authorName:string; body:string; internalNote:boolean; createdAt:string };
export type TicketEvent = { id:string; ticketId:string; eventType:string; fromValue?:string; toValue?:string; actorName?:string; createdAt:string };
export type TicketDetail = { ticket:SupportTicket; comments:TicketComment[]; history:TicketEvent[] };
export type PaymentGateway = { id:string; tenantId:string; provider:string; enabled:boolean; publicKey?:string; secretConfigured:boolean; upiVpa?:string; payeeName?:string; updatedAt:string };
export type Wallet = { id:string; tenantId:string; userId:string; balance:number; currency:string; status:string; updatedAt:string };
export type WalletEntry = { id:string; walletId:string; amount:number; balanceAfter:number; reason:string; reference?:string; createdAt:string };
export type ScanPayOrder = { id:string; orderNumber:string; tenantId:string; userId:string; stationId?:string; chargerId?:string; amount:number; currency:string; status:string; paymentId?:string; sessionId?:string; payload:string; createdAt:string };
export type Invoice = { id:string;tenantId:string;userId:string;billId:string;invoiceNumber:string;customerName:string;customerEmail:string;subtotal:number;taxAmount:number;totalAmount:number;currency:string;status:string;issueDate:string;dueDate:string;createdAt:string };
export type Bill = { id:string;tenantId:string;userId:string;billNumber:string;subtotal:number;taxAmount:number;totalAmount:number;currency:string;status:string;createdAt:string };
export type Partner = {id:string;tenantId:string;companyName:string;contactName:string;email?:string;phone?:string;commissionPercent:number;status:string;createdAt:string};
export type Technician = {id:string;tenantId:string;name:string;email:string;phone?:string;skills?:string;status:string;createdAt:string};
export type RfidCard = {id:string;tenantId:string;cardUid:string;label?:string;userId?:string;notes?:string;status:string;issuedAt:string;revokedAt?:string};
export type MaintenanceJob={id:string;jobNumber:string;tenantId:string;title:string;description?:string;type:string;stationId?:string;chargerId?:string;technicianId?:string;technicianName?:string;scheduledAt:string;status:string;createdAt:string};
export type AmcContract={id:string;contractNumber:string;tenantId:string;partnerId:string;stationId:string;startDate:string;endDate:string;amount:number;currency:string;status:string;createdAt:string};
export type RolePolicy={id:string;tenantId:string;roleName:string;permissionList:string[];systemRole:boolean;updatedAt:string};
export type Administrator={id:string;tenantId:string;authUserId?:string;name:string;email:string;phone?:string;roleName:string;status:string;createdAt:string};
export type AdminApiKey={id:string;tenantId:string;keyId:string;secret?:string;name:string;roleName:string;scopes:string;status:string;createdAt:string};
export type Tariff={id:string;tenantId:string;code:string;name:string;energyPricePerKwh:number;timePricePerMinute:number;sessionFee:number;taxPercent:number;currency:string;status:string;validFrom:string;validTo?:string};

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
  realtimeUrl: (tenantId: string) => `${API_BASE}/api/v1/admin/events?tenantId=${encodeURIComponent(tenantId)}`,
  login: (email: string, password: string) => request<TokenResponse>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (email: string, password: string) => request<TokenResponse>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  tenants: async () => pageContent(await request<{ content: Tenant[] }>('/api/v1/tenants?page=0&size=100')),
  chargers: (tenantId: string) => request<Charger[]>(`/api/v1/chargers?tenantId=${encodeURIComponent(tenantId)}`),
  sessions: (tenantId: string) => request<ChargingSession[]>(`/api/v1/charging-sessions?tenantId=${encodeURIComponent(tenantId)}`),
  payments: (tenantId: string) => request<Payment[]>(`/api/v1/payments?tenantId=${encodeURIComponent(tenantId)}`),
  refundPayment: (id: string) => request<Payment>(`/api/v1/payments/${id}/refund`, { method: 'POST' }),
  bills: (tenantId:string)=>request<Bill[]>(`/api/v1/bills?tenantId=${encodeURIComponent(tenantId)}`),
  invoices: (tenantId:string)=>request<Invoice[]>(`/api/v1/invoices?tenantId=${encodeURIComponent(tenantId)}`),
  createInvoice: (body:Record<string,unknown>)=>request<Invoice>('/api/v1/invoices',{method:'POST',body:JSON.stringify(body)}),
  invoiceAction: (id:string,action:'issue'|'pay'|'void')=>request<Invoice>(`/api/v1/invoices/${id}/${action}`,{method:'POST'}),
  paymentGateways:(tenantId:string)=>request<PaymentGateway[]>(`/api/v1/payments/operations/gateways?tenantId=${encodeURIComponent(tenantId)}`),
  savePaymentGateway:(tenantId:string,body:Record<string,unknown>)=>request<PaymentGateway>(`/api/v1/payments/operations/gateways?tenantId=${encodeURIComponent(tenantId)}`,{method:'PUT',body:JSON.stringify(body)}),
  wallets:(tenantId:string)=>request<Wallet[]>(`/api/v1/payments/operations/wallets?tenantId=${encodeURIComponent(tenantId)}`),
  createWallet:(body:{tenantId:string;userId:string;currency:string})=>request<Wallet>('/api/v1/payments/operations/wallets',{method:'POST',body:JSON.stringify(body)}),
  walletEntries:(id:string)=>request<WalletEntry[]>(`/api/v1/payments/operations/wallets/${id}/entries`),
  adjustWallet:(id:string,body:{amount:number;reason:string;reference?:string})=>request<Wallet>(`/api/v1/payments/operations/wallets/${id}/adjustments`,{method:'POST',body:JSON.stringify(body)}),
  scanPayOrders:(tenantId:string)=>request<ScanPayOrder[]>(`/api/v1/payments/operations/scan-pay-orders?tenantId=${encodeURIComponent(tenantId)}`),
  createScanPayOrder:(body:Record<string,unknown>)=>request<ScanPayOrder>('/api/v1/payments/operations/scan-pay-orders',{method:'POST',body:JSON.stringify(body)}),
  users: async (tenantId: string) => pageContent(await request<{ content: UserProfile[] }>(`/api/v1/users?tenantId=${encodeURIComponent(tenantId)}&page=0&size=100`)),
  createUser: (body: { authUserId: string; tenantId: string; firstName: string; lastName: string; email: string; phone?: string }) => request<UserProfile>('/api/v1/users', { method: 'POST', body: JSON.stringify(body) }),
  deactivateUser: (id: string) => request<void>(`/api/v1/users/${id}`, { method: 'DELETE' }),
  partners:(tenantId:string)=>request<Partner[]>(`/api/v1/users/directory/partners?tenantId=${encodeURIComponent(tenantId)}`),
  savePartner:(body:Record<string,unknown>,id?:string)=>request<Partner>(id?`/api/v1/users/directory/partners/${id}`:'/api/v1/users/directory/partners',{method:id?'PUT':'POST',body:JSON.stringify(body)}),
  deletePartner:(id:string)=>request<void>(`/api/v1/users/directory/partners/${id}`,{method:'DELETE'}),
  technicians:(tenantId:string)=>request<Technician[]>(`/api/v1/users/directory/technicians?tenantId=${encodeURIComponent(tenantId)}`),
  saveTechnician:(body:Record<string,unknown>,id?:string)=>request<Technician>(id?`/api/v1/users/directory/technicians/${id}`:'/api/v1/users/directory/technicians',{method:id?'PUT':'POST',body:JSON.stringify(body)}),
  deleteTechnician:(id:string)=>request<void>(`/api/v1/users/directory/technicians/${id}`,{method:'DELETE'}),
  rfidCards:(tenantId:string)=>request<RfidCard[]>(`/api/v1/users/directory/rfid-cards?tenantId=${encodeURIComponent(tenantId)}`),
  issueRfidCard:(body:Record<string,unknown>)=>request<RfidCard>('/api/v1/users/directory/rfid-cards',{method:'POST',body:JSON.stringify(body)}),
  setRfidStatus:(id:string,status:string)=>request<RfidCard>(`/api/v1/users/directory/rfid-cards/${id}/status`,{method:'PATCH',body:JSON.stringify({status})}),
  tariffs:(tenantId:string)=>request<Tariff[]>(`/api/v1/tariffs?tenantId=${encodeURIComponent(tenantId)}`),
  createTariff:(body:Record<string,unknown>)=>request<Tariff>('/api/v1/tariffs',{method:'POST',body:JSON.stringify(body)}),
  setTariffStatus:(id:string,status:string)=>request<Tariff>(`/api/v1/tariffs/${id}/status`,{method:'PATCH',body:JSON.stringify({status})}),
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
  setChargerStatus: (id: string, status: string) => request<Charger>(`/api/v1/chargers/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),
  heartbeatCharger: (id: string) => request<Charger>(`/api/v1/chargers/${id}/heartbeat`, { method: 'POST' }),
  connectors: (chargerId: string) => request<Connector[]>(`/api/v1/connectors?chargerId=${encodeURIComponent(chargerId)}`),
  createConnector: (body: { tenantId: string; chargerId: string; connectorNumber: number; type: string; maxPowerKw: number; maxVoltage: number; maxCurrent: number }) => request<Connector>('/api/v1/connectors', { method: 'POST', body: JSON.stringify(body) }),
  ocppConnections: () => request<OcppConnection[]>('/api/v1/ocpp/connections'),
  ocppMessages: () => request<OcppMessage[]>('/api/v1/ocpp/messages'),
  remoteStart: (body: { stationId: string; ocppVersion: string; connectorId: number; idToken: string }) => request<{messageId:string}>('/api/v1/ocpp/commands/remote-start', { method: 'POST', body: JSON.stringify(body) }),
  remoteStop: (body: { stationId: string; ocppVersion: string; transactionId: string }) => request<{messageId:string}>('/api/v1/ocpp/commands/remote-stop', { method: 'POST', body: JSON.stringify(body) }),
  latestTelemetry: (connectorId: string) => request<Telemetry[]>(`/api/v1/telemetry/readings/latest?connectorId=${encodeURIComponent(connectorId)}`),
  firmwarePackages: () => request<FirmwarePackage[]>('/api/v1/firmware/packages'),
  createFirmwarePackage: (body: { vendor: string; model: string; version: string; downloadUrl: string; checksum: string; checksumAlgorithm: string; signature?: string; fileSizeBytes: number }) => request<FirmwarePackage>('/api/v1/firmware/packages', { method: 'POST', body: JSON.stringify(body) }),
  firmwareJobs: (tenantId: string) => request<FirmwareJob[]>(`/api/v1/firmware/jobs?tenantId=${encodeURIComponent(tenantId)}`),
  createFirmwareJob: (body: { tenantId: string; chargerId: string; firmwarePackageId: string; ocppVersion: string; scheduledAt: string }) => request<FirmwareJob>('/api/v1/firmware/jobs', { method: 'POST', body: JSON.stringify(body) }),
  dispatchFirmwareJob: (id: string) => request<FirmwareJob>(`/api/v1/firmware/jobs/${id}/dispatch`, { method: 'POST' }),
  supportTickets: (tenantId:string) => request<SupportTicket[]>(`/api/v1/support/tickets?tenantId=${encodeURIComponent(tenantId)}`),
  supportTicket: (id:string) => request<TicketDetail>(`/api/v1/support/tickets/${id}`),
  createSupportTicket: (body:{tenantId:string;requesterId?:string;requesterName:string;requesterEmail:string;subject:string;description:string;category:string;priority:string;stationId?:string;sessionId?:string}) => request<SupportTicket>('/api/v1/support/tickets',{method:'POST',body:JSON.stringify(body)}),
  assignSupportTicket: (id:string,body:{assigneeId:string;assigneeName:string;actorId?:string;actorName?:string}) => request<SupportTicket>(`/api/v1/support/tickets/${id}/assignment`,{method:'PATCH',body:JSON.stringify(body)}),
  prioritizeSupportTicket: (id:string,body:{priority:string;actorId?:string;actorName?:string}) => request<SupportTicket>(`/api/v1/support/tickets/${id}/priority`,{method:'PATCH',body:JSON.stringify(body)}),
  transitionSupportTicket: (id:string,body:{status:string;actorId?:string;actorName?:string}) => request<SupportTicket>(`/api/v1/support/tickets/${id}/status`,{method:'PATCH',body:JSON.stringify(body)}),
  commentSupportTicket: (id:string,body:{authorId?:string;authorName:string;body:string;internalNote:boolean}) => request<TicketComment>(`/api/v1/support/tickets/${id}/comments`,{method:'POST',body:JSON.stringify(body)}),
  maintenanceJobs:(tenantId:string)=>request<MaintenanceJob[]>(`/api/v1/support/maintenance-jobs?tenantId=${encodeURIComponent(tenantId)}`),
  createMaintenanceJob:(body:Record<string,unknown>)=>request<MaintenanceJob>('/api/v1/support/maintenance-jobs',{method:'POST',body:JSON.stringify(body)}),
  setMaintenanceStatus:(id:string,status:string)=>request<MaintenanceJob>(`/api/v1/support/maintenance-jobs/${id}/status`,{method:'PATCH',body:JSON.stringify({status})}),
  amcContracts:(tenantId:string)=>request<AmcContract[]>(`/api/v1/support/amc-contracts?tenantId=${encodeURIComponent(tenantId)}`),
  createAmcContract:(body:Record<string,unknown>)=>request<AmcContract>('/api/v1/support/amc-contracts',{method:'POST',body:JSON.stringify(body)}),
  setAmcStatus:(id:string,status:string)=>request<AmcContract>(`/api/v1/support/amc-contracts/${id}/status`,{method:'PATCH',body:JSON.stringify({status})}),
  serviceStatus: async (slug: string) => { const response = await fetch(`${API_BASE}/openapi/${slug}/v3/api-docs`); return response.ok; },
  roles:(tenantId:string)=>request<RolePolicy[]>(`/api/v1/admin/governance/roles?tenantId=${encodeURIComponent(tenantId)}`),
  saveRole:(tenantId:string,body:{roleName:string;permissions:string[]})=>request<RolePolicy>(`/api/v1/admin/governance/roles?tenantId=${encodeURIComponent(tenantId)}`,{method:'PUT',body:JSON.stringify(body)}),
  administrators:(tenantId:string)=>request<Administrator[]>(`/api/v1/admin/governance/administrators?tenantId=${encodeURIComponent(tenantId)}`),
  saveAdministrator:(tenantId:string,body:Record<string,unknown>,id?:string)=>request<Administrator>(id?`/api/v1/admin/governance/administrators/${id}?tenantId=${encodeURIComponent(tenantId)}`:`/api/v1/admin/governance/administrators?tenantId=${encodeURIComponent(tenantId)}`,{method:id?'PUT':'POST',body:JSON.stringify(body)}),
  deleteAdministrator:(id:string)=>request<void>(`/api/v1/admin/governance/administrators/${id}`,{method:'DELETE'}),
  adminApiKeys:(tenantId:string)=>request<AdminApiKey[]>(`/api/v1/admin/governance/api-keys?tenantId=${encodeURIComponent(tenantId)}`),
  createAdminApiKey:(tenantId:string,body:Record<string,unknown>)=>request<AdminApiKey>(`/api/v1/admin/governance/api-keys?tenantId=${encodeURIComponent(tenantId)}`,{method:'POST',body:JSON.stringify(body)}),
  disableAdminApiKey:(id:string)=>request<AdminApiKey>(`/api/v1/admin/governance/api-keys/${id}/disable`,{method:'POST'}),
  governanceSettings:(tenantId:string)=>request<Record<string,string>>(`/api/v1/admin/governance/settings?tenantId=${encodeURIComponent(tenantId)}`),
  saveGovernanceSettings:(tenantId:string,body:Record<string,string>)=>request<Record<string,string>>(`/api/v1/admin/governance/settings?tenantId=${encodeURIComponent(tenantId)}`,{method:'PUT',body:JSON.stringify(body)}),
};
