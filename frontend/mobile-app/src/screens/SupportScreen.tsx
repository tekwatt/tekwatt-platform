import { useCallback, useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, Field, PageHeader, Pill, PrimaryButton, Screen, SecondaryButton, commonStyles } from '../components/ui';
import { colors } from '../theme';
import type { SupportTicket } from '../types';

const priorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
const categories = ['CHARGING', 'PAYMENT', 'STATION', 'ACCOUNT', 'OTHER'];

export function SupportScreen() {
  const { token, tenant, profile, claims } = useAuth();
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [show, setShow] = useState(false);
  const [form, setForm] = useState({ subject: '', description: '', category: 'CHARGING', priority: 'MEDIUM' });
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const load = useCallback(async () => { if (!token || !tenant) return; setLoading(true); setError(''); try { const data = await api.tickets(tenant.id, token); setTickets(data.filter(item => item.requesterId === profile?.id || item.requesterEmail.toLowerCase() === claims?.email?.toLowerCase())); } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to load support tickets.'); } finally { setLoading(false); } }, [claims, profile, tenant, token]);
  useFocusEffect(useCallback(() => { void load(); }, [load]));
  const submit = async () => { if (!token || !tenant || !claims) return; if (!form.subject.trim() || !form.description.trim()) return setError('Enter a subject and description.'); setSaving(true); setError(''); try { await api.createTicket({ tenantId: tenant.id, requesterId: profile?.id, requesterName: profile?.fullName || `${profile?.firstName || ''} ${profile?.lastName || ''}`.trim() || claims.email || 'Customer', requesterEmail: profile?.email || claims.email, ...form }, token); setForm({ subject: '', description: '', category: 'CHARGING', priority: 'MEDIUM' }); setShow(false); await load(); } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to create support ticket.'); } finally { setSaving(false); } };
  return <Screen refreshing={loading} onRefresh={load}>
    <PageHeader eyebrow="HELP CENTRE" title="Support" subtitle="Raise an issue and follow it through the complete support lifecycle." action={<PrimaryButton compact label="New ticket" onPress={() => setShow(true)}/>}/>
    {error && !show ? <ErrorBanner message={error}/> : null}
    {tickets.length ? tickets.map(ticket => <Card key={ticket.id}><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{ticket.subject}</Text><Text style={commonStyles.tiny}>{ticket.ticketNumber} · {new Date(ticket.createdAt).toLocaleDateString()}</Text></View><Pill label={ticket.status} tone={['RESOLVED', 'CLOSED'].includes(ticket.status) ? 'green' : ticket.priority === 'URGENT' ? 'red' : 'amber'}/></View><Text style={[commonStyles.body, styles.description]} numberOfLines={3}>{ticket.description}</Text><View style={styles.tags}><Pill label={ticket.category}/><Pill label={ticket.priority} tone={ticket.priority === 'URGENT' ? 'red' : 'neutral'}/></View></Card>) : <EmptyState title="No support tickets" message="If you encounter a charging, payment, station or account problem, create a ticket here."/>}
    <Modal animationType="slide" transparent visible={show} onRequestClose={() => setShow(false)}><View style={styles.backdrop}><View style={styles.sheet}><View style={commonStyles.between}><View><Text style={styles.sheetTitle}>New support ticket</Text><Text style={commonStyles.body}>Tell the TekWatt team what happened.</Text></View><SecondaryButton label="Close" onPress={() => setShow(false)}/></View>{error ? <ErrorBanner message={error}/> : null}<Field label="Subject" value={form.subject} onChangeText={subject => setForm(current => ({ ...current, subject }))} maxLength={200}/><Field label="Description" value={form.description} onChangeText={description => setForm(current => ({ ...current, description }))} multiline/><Text style={styles.choiceLabel}>Category</Text><View style={styles.choices}>{categories.map(value => <Pressable key={value} onPress={() => setForm(current => ({ ...current, category: value }))} style={[styles.choice, form.category === value && styles.choiceActive]}><Text style={[styles.choiceText, form.category === value && styles.choiceTextActive]}>{value}</Text></Pressable>)}</View><Text style={styles.choiceLabel}>Priority</Text><View style={styles.choices}>{priorities.map(value => <Pressable key={value} onPress={() => setForm(current => ({ ...current, priority: value }))} style={[styles.choice, form.priority === value && styles.choiceActive]}><Text style={[styles.choiceText, form.priority === value && styles.choiceTextActive]}>{value}</Text></Pressable>)}</View><PrimaryButton label="Create ticket" loading={saving} onPress={() => void submit()}/></View></View></Modal>
  </Screen>;
}

const styles = StyleSheet.create({ flex: { flex: 1 }, description: { marginTop: 12 }, tags: { flexDirection: 'row', gap: 7, marginTop: 12 }, backdrop: { flex: 1, justifyContent: 'flex-end', backgroundColor: '#00101880' }, sheet: { backgroundColor: colors.background, borderTopLeftRadius: 26, borderTopRightRadius: 26, padding: 18, gap: 14 }, sheetTitle: { color: colors.ink, fontSize: 21, fontWeight: '900' }, choiceLabel: { color: colors.ink, fontSize: 12, fontWeight: '900' }, choices: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 }, choice: { borderWidth: 1, borderColor: colors.line, borderRadius: 999, paddingVertical: 7, paddingHorizontal: 11, backgroundColor: colors.white }, choiceActive: { borderColor: colors.blue, backgroundColor: colors.surfaceSoft }, choiceText: { color: colors.muted, fontSize: 10, fontWeight: '800' }, choiceTextActive: { color: colors.blue } });
