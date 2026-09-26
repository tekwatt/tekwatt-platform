export function paymentLink(invoiceId:string,tenantId:string):string|undefined {
  const base=(process.env.EXPO_PUBLIC_CUSTOMER_PORTAL_URL||'').replace(/\/+$/,'');
  if(!/^https:\/\/[^/?#@]+(?:\/[^?#]*)?$/.test(base))return undefined;
  return `${base}/?invoice=${encodeURIComponent(invoiceId)}&workspace=${encodeURIComponent(tenantId)}#/payments/invoices`;
}
