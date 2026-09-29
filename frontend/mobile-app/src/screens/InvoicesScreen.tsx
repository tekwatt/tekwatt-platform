import { useCallback, useState } from 'react';
import { Modal, ScrollView, Share, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { api } from '../api/client';
import { payInvoice } from '../api/payInvoice';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, PageHeader, Pill, PrimaryButton, Screen, SecondaryButton, commonStyles } from '../components/ui';
import { colors } from '../theme';
import type { Bill, Charger, ChargingSession, Invoice } from '../types';

type InvoiceDetail = { invoice: Invoice; bill: Bill | null; session: ChargingSession | null; charger: Charger | null };
const money = (value: number | undefined) => `₹${Number(value || 0).toFixed(2)}`;

export function InvoicesScreen() {
  const { token, tenant, profile } = useAuth();
  const [items, setItems] = useState<Invoice[]>([]);
  const [detail, setDetail] = useState<InvoiceDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [payingId, setPayingId] = useState('');
  const load = useCallback(async () => {
    if (!token || !tenant || !profile) return;
    setLoading(true); setError('');
    try {
      const rows = await api.myInvoices(token);
      setItems(rows.sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to load invoices.'); }
    finally { setLoading(false); }
  }, [profile, tenant, token]);
  useFocusEffect(useCallback(() => { void load(); }, [load]));

  const pay = async (invoice: Invoice) => {
    if (!token || !profile || payingId) return;
    setPayingId(invoice.id); setError(''); setNotice('');
    try { setNotice(await payInvoice(invoice, profile, token)); await load(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Payment could not be completed.'); }
    finally { setPayingId(''); }
  };
  const view = async (invoice: Invoice) => {
    if (!token || !tenant || !profile || invoice.userId !== profile.id) return;
    setDetailsLoading(true); setError('');
    try {
      const bill = invoice.billId ? await api.myBill(invoice.billId, token) : null;
      if (bill && (bill.userId !== profile.id || bill.tenantId !== tenant.id)) throw new Error('This invoice does not belong to your account.');
      const session = bill?.sessionId ? (await api.sessions(tenant.id, token)).find(item => item.id === bill.sessionId) || null : null;
      if (session && session.userId !== profile.id) throw new Error('This charging session does not belong to your account.');
      const charger = session ? (await api.myChargers(token)).find(item => item.id === session.chargerId) || null : null;
      setDetail({ invoice, bill, session, charger });
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to show invoice details.'); }
    finally { setDetailsLoading(false); }
  };
  const share = async () => {
    if (!detail) return;
    const { invoice, bill, session, charger } = detail;
    await Share.share({ message: [
      `TekWatt invoice ${invoice.invoiceNumber}`, `Customer: ${invoice.customerName}`,
      `Station: ${charger?.stationName || charger?.stationId || 'Not recorded'}`,
      `Charging: ${session?.startedAt ? new Date(session.startedAt).toLocaleString() : 'Not recorded'} to ${session?.stoppedAt ? new Date(session.stoppedAt).toLocaleString() : 'Not recorded'}`,
      `Duration: ${bill?.durationMinutes ?? 'Not recorded'} min · Energy: ${bill?.energyKwh ?? 'Not recorded'} kWh`,
      `Subtotal ${money(invoice.subtotal)} · Tax ${money(invoice.taxAmount)} · Total ${money(invoice.totalAmount)}`,
      `Status: ${invoice.status}`,
    ].join('\n') });
  };
  return <Screen refreshing={loading} onRefresh={load}>
    <PageHeader eyebrow="BILLING" title="Invoices" subtitle="Review charging details and pay securely in the app."/>
    {error ? <ErrorBanner message={error}/> : null}{notice ? <Text style={commonStyles.body}>{notice}</Text> : null}
    {items.length ? items.map(item => <Card key={item.id}>
      <View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{item.invoiceNumber}</Text><Text style={commonStyles.tiny}>{item.issueDate || new Date(item.createdAt).toLocaleDateString()} · Due {item.dueDate || 'not set'}</Text></View><Pill label={item.status} tone={item.status === 'PAID' ? 'green' : item.status === 'VOID' ? 'neutral' : 'amber'}/></View>
      <View style={styles.amountRow}><View><Text style={commonStyles.tiny}>Subtotal</Text><Text style={commonStyles.body}>{money(item.subtotal)}</Text></View><View><Text style={commonStyles.tiny}>Tax</Text><Text style={commonStyles.body}>{money(item.taxAmount)}</Text></View><View><Text style={commonStyles.tiny}>Total</Text><Text style={styles.total}>{money(item.totalAmount)}</Text></View></View>
      <View style={styles.action}><SecondaryButton label="View charging details" disabled={detailsLoading} onPress={() => void view(item)}/></View>
      {['ISSUED', 'OVERDUE'].includes(item.status) && Number(item.totalAmount) > 0 ? <View style={styles.action}><PrimaryButton label="Pay securely in app" loading={payingId === item.id} disabled={Boolean(payingId)} onPress={() => void pay(item)}/></View> : null}
    </Card>) : <EmptyState title="No invoices" message="Issued charging invoices will appear here."/>}
    <Modal transparent animationType="slide" visible={Boolean(detail)} onRequestClose={() => setDetail(null)}><View style={styles.backdrop}><ScrollView style={styles.sheet} contentContainerStyle={styles.detailContent}>
      <View style={commonStyles.between}><Text style={styles.detailTitle}>Invoice details</Text><SecondaryButton label="Close" onPress={() => setDetail(null)}/></View>
      {detail ? <><Text style={commonStyles.strong}>{detail.invoice.invoiceNumber} · {detail.invoice.status}</Text>
        <Line label="Customer" value={detail.invoice.customerName}/><Line label="Email" value={detail.invoice.customerEmail}/>
        <Line label="Billing address" value={detail.invoice.billingAddress || 'Not recorded'}/><Line label="Tax registration" value={detail.invoice.taxRegistrationNumber || 'Not recorded'}/>
        <Line label="Station" value={detail.charger?.stationName || detail.charger?.stationId || 'Not recorded'}/>
        <Line label="Charging started" value={detail.session?.startedAt ? new Date(detail.session.startedAt).toLocaleString() : 'Not recorded'}/>
        <Line label="Charging stopped" value={detail.session?.stoppedAt ? new Date(detail.session.stoppedAt).toLocaleString() : 'Not recorded'}/>
        <Line label="Duration" value={detail.bill ? `${detail.bill.durationMinutes} min` : 'Not recorded'}/>
        <Line label="Energy delivered" value={detail.bill ? `${Number(detail.bill.energyKwh).toFixed(2)} kWh` : 'Not recorded'}/>
        <Line label="Energy charge" value={detail.bill ? money(detail.bill.energyAmount) : 'Not recorded'}/>
        <Line label="Time charge" value={detail.bill ? money(detail.bill.timeAmount) : 'Not recorded'}/>
        <Line label="Session fee" value={detail.bill ? money(detail.bill.sessionFee) : 'Not recorded'}/>
        <Line label="Subtotal" value={money(detail.invoice.subtotal)}/><Line label="Tax" value={money(detail.invoice.taxAmount)}/><Line label="Total" value={money(detail.invoice.totalAmount)}/>
        <SecondaryButton label="Share invoice summary" onPress={() => void share()}/><Text style={commonStyles.tiny}>This shares a text summary, not a GST PDF invoice.</Text>
      </> : null}
    </ScrollView></View></Modal>
  </Screen>;
}

function Line({ label, value }: { label: string; value: string }) { return <View style={styles.line}><Text style={commonStyles.tiny}>{label}</Text><Text style={commonStyles.body}>{value}</Text></View>; }
const styles = StyleSheet.create({ flex: { flex: 1 }, amountRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 16 }, action: { marginTop: 14 }, total: { color: colors.blue, fontSize: 17, fontWeight: '900' }, backdrop: { flex: 1, justifyContent: 'flex-end', backgroundColor: '#00101880' }, sheet: { maxHeight: '88%', backgroundColor: colors.background, borderTopLeftRadius: 24, borderTopRightRadius: 24 }, detailContent: { padding: 20, gap: 10 }, detailTitle: { color: colors.ink, fontSize: 21, fontWeight: '900' }, line: { borderBottomWidth: 1, borderBottomColor: colors.line, paddingVertical: 6, gap: 3 } });
