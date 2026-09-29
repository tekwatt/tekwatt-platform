import { useCallback, useEffect, useState } from 'react';
import { Linking, Pressable, StyleSheet, Text, View } from 'react-native';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, Metric, PageHeader, Pill, Screen, SecondaryButton, commonStyles } from '../components/ui';
import { colors } from '../theme';
import type { CpoOverview } from '../types';

type Section = 'Overview' | 'Chargers' | 'Sessions' | 'Account';
const sections: Section[] = ['Overview', 'Chargers', 'Sessions', 'Account'];
const portalUrl = process.env.EXPO_PUBLIC_CUSTOMER_PORTAL_URL?.replace(/\/$/, '');

export function CpoScreen() {
  const { token, cpo, tenant, signOut } = useAuth();
  const [section, setSection] = useState<Section>('Overview');
  const [overview, setOverview] = useState<CpoOverview | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const load = useCallback(async (silent = false) => {
    if (!token) return;
    if (!silent) setLoading(true);
    try { setOverview(await api.cpoOverview(token)); setError(''); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to load CPO operations.'); }
    finally { if (!silent) setLoading(false); }
  }, [token]);
  useEffect(() => { void load(); const timer = setInterval(() => void load(true), 20_000); return () => clearInterval(timer); }, [load]);

  const chargers = overview?.chargers ?? [];
  const sessions = overview?.sessions ?? [];
  const active = sessions.filter(item => ['ACTIVE', 'CHARGING', 'IN_PROGRESS', 'STARTED'].includes(item.status.toUpperCase()));
  const delivered = sessions.filter(item => item.status === 'COMPLETED').reduce((sum, item) => sum + item.energyKwh, 0);
  const stationCount = new Set(chargers.map(item => item.stationId)).size;
  const nameOf = (chargerId: string) => chargers.find(item => item.id === chargerId)?.stationName || chargerId;

  return <Screen refreshing={loading} onRefresh={load}>
    <PageHeader eyebrow="CPO OPERATIONS" title={cpo?.companyName || 'Operator'} subtitle={`Your assigned charging network · ${tenant?.name || 'TekWatt'}`}/>
    <View style={styles.tabs}>{sections.map(item => <Pressable key={item} accessibilityRole="tab" accessibilityState={{ selected: item === section }} onPress={() => setSection(item)} style={[styles.tab, item === section && styles.activeTab]}><Text style={[styles.tabText, item === section && styles.activeText]}>{item}</Text></Pressable>)}</View>
    {error ? <ErrorBanner message={error}/> : null}
    {section === 'Overview' && <>
      <View style={styles.metrics}><Metric label="STATIONS" value={String(stationCount)} hint="Assigned sites"/><Metric label="CHARGERS" value={String(chargers.length)} hint="Assigned equipment"/><Metric label="ACTIVE" value={String(active.length)} hint="Live sessions"/><Metric label="ENERGY" value={`${delivered.toFixed(1)} kWh`} hint="Completed sessions"/></View>
      <Text style={commonStyles.sectionTitle}>Needs attention</Text>
      {chargers.filter(item => ['FAULTED', 'OFFLINE', 'UNAVAILABLE'].includes(item.status.toUpperCase())).length ? chargers.filter(item => ['FAULTED', 'OFFLINE', 'UNAVAILABLE'].includes(item.status.toUpperCase())).map(item => <ChargerCard key={item.id} charger={item}/>) : <EmptyState title="No reported charger faults" message="Refresh to check the latest charger status."/>}
      <Text style={commonStyles.sectionTitle}>Charging now</Text>
      {active.length ? active.slice(0, 5).map(item => <SessionCard key={item.id} session={item} stationName={nameOf(item.chargerId)}/>) : <EmptyState title="No active sessions" message="New charging sessions will appear here."/>}
    </>}
    {section === 'Chargers' && <><Text style={commonStyles.sectionTitle}>Your assigned chargers</Text>{chargers.length ? chargers.map(item => <ChargerCard key={item.id} charger={item}/>) : <EmptyState title="No chargers assigned" message="Ask the TekWatt administrator to assign stations to your CPO account."/>}</>}
    {section === 'Sessions' && <><Text style={commonStyles.sectionTitle}>Recent charging sessions</Text>{sessions.length ? sessions.slice().sort((a, b) => (b.startedAt || '').localeCompare(a.startedAt || '')).slice(0, 30).map(item => <SessionCard key={item.id} session={item} stationName={nameOf(item.chargerId)}/>) : <EmptyState title="No charging sessions" message="Sessions on your assigned chargers will appear here."/>}</>}
    {section === 'Account' && <><Card><Text style={commonStyles.strong}>{cpo?.companyName}</Text><Text style={commonStyles.body}>{cpo?.contactName}</Text><Text style={commonStyles.body}>{cpo?.email}</Text><View style={commonStyles.divider}/><Text style={commonStyles.tiny}>Workspace: {tenant?.name}</Text><Text style={commonStyles.tiny}>Partner ID: {cpo?.partnerId}</Text></Card><Text style={commonStyles.body}>Station setup, tariffs, remote operations and settlement reports remain in the CPO web portal.</Text>{portalUrl ? <SecondaryButton label="Open CPO web portal" onPress={() => void Linking.openURL(portalUrl)}/> : null}<SecondaryButton label="Sign out" onPress={() => void signOut()}/></>}
  </Screen>;
}

function ChargerCard({ charger }: { charger: CpoOverview['chargers'][number] }) {
  const tone = charger.status === 'AVAILABLE' ? 'green' : charger.status === 'FAULTED' ? 'red' : 'amber';
  return <Card><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{charger.stationName || charger.stationId}</Text><Text style={commonStyles.tiny}>{charger.stationId} · {charger.city || 'Location not set'}</Text></View><Pill label={charger.status} tone={tone}/></View><Text style={commonStyles.body}>{charger.powerKw.toFixed(1)} kW · {charger.lastHeartbeat ? `Last heartbeat ${new Date(charger.lastHeartbeat).toLocaleString()}` : 'No heartbeat reported'}</Text></Card>;
}

function SessionCard({ session, stationName }: { session: CpoOverview['sessions'][number]; stationName: string }) {
  return <Card><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{stationName}</Text><Text style={commonStyles.tiny}>{session.transactionId}</Text></View><Pill label={session.status} tone={session.status === 'COMPLETED' ? 'green' : 'amber'}/></View><Text style={commonStyles.body}>{session.energyKwh.toFixed(2)} kWh · {session.startedAt ? new Date(session.startedAt).toLocaleString() : 'Start time unavailable'}</Text></Card>;
}

const styles = StyleSheet.create({ flex: { flex: 1 }, tabs: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 }, tab: { paddingVertical: 9, paddingHorizontal: 11, borderRadius: 12, backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line }, activeTab: { backgroundColor: colors.blue, borderColor: colors.blue }, tabText: { color: colors.muted, fontSize: 12, fontWeight: '800' }, activeText: { color: colors.white }, metrics: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', rowGap: 12 } });
