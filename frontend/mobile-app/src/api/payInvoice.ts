import { api } from './client';
import type { Invoice, UserProfile } from '../types';

export async function payInvoice(invoice: Invoice, profile: UserProfile, token: string): Promise<string> {
  if (invoice.userId !== profile.id || !['ISSUED', 'OVERDUE'].includes(invoice.status)) {
    throw new Error('This invoice is not payable by your account. Refresh your invoices.');
  }
  if (!invoice.billId || !Number.isFinite(Number(invoice.totalAmount)) || Number(invoice.totalAmount) <= 0) {
    throw new Error('This invoice has no valid payable amount. Contact support.');
  }

  // A verified payment can outlive the invoice status update. Never open a second checkout for it.
  const previous = (await api.myPayments(token)).find(
    payment => payment.invoiceId === invoice.id && payment.userId === profile.id && payment.status === 'SUCCEEDED',
  );
  if (previous) {
    await api.settleMyInvoice(invoice.id, token);
    return 'Your existing payment was confirmed and the invoice is now paid.';
  }

  let checkout: typeof import('react-native-razorpay').default;
  try {
    checkout = (await import('react-native-razorpay')).default;
  } catch {
    throw new Error('In-app checkout needs a TekWatt Android build. It is not available in Expo Go.');
  }

  const order = await api.myRazorpayOrder(invoice.id, token);
  if (!order.keyId || !order.orderId || order.amount <= 0) throw new Error('The payment provider is not configured for this invoice.');

  let result: { razorpay_payment_id?:string; razorpay_order_id?:string; razorpay_signature?:string };
  try {
    result = await checkout.open({
      key: order.keyId,
      order_id: order.orderId,
      amount: order.amount,
      currency: order.currency,
      name: 'TekWatt Nexus',
      description: order.description || `Invoice ${invoice.invoiceNumber}`,
      prefill: { name: invoice.customerName || profile.fullName || '', email: invoice.customerEmail || profile.email, contact: profile.phone || '' },
      theme: { color: '#029ed3' },
    });
  } catch (reason) {
    const message = reason && typeof reason === 'object' && 'description' in reason ? String(reason.description) : '';
    throw new Error(message || 'Payment was cancelled or could not be completed.');
  }
  if (!result.razorpay_payment_id || !result.razorpay_order_id || !result.razorpay_signature) {
    throw new Error('Payment returned incomplete confirmation. Do not retry yet; check your payment history.');
  }
  const verified = await api.verifyMyRazorpayPayment({
    paymentId: order.paymentId,
    razorpayPaymentId: result.razorpay_payment_id,
    razorpayOrderId: result.razorpay_order_id,
    razorpaySignature: result.razorpay_signature,
  }, token);
  if (verified.status !== 'SUCCEEDED') throw new Error('Payment is not yet confirmed. Do not pay again until its status is checked.');
  try {
    await api.settleMyInvoice(invoice.id, token);
  } catch {
    throw new Error(`Payment ${verified.id} is verified, but the invoice has not updated yet. Do not pay again; contact support.`);
  }
  return 'Payment verified. Your invoice is paid.';
}
