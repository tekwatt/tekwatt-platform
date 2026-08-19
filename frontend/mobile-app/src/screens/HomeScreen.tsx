import { useCallback, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, Metric, PageHeader, Pill, Screen, commonStyles } from '../components/ui';
import { colors } from '../theme';
import type { Charger, ChargingSession, Wallet } from '../types';

export function HomeScreen() {
  const { token, tenant, profile } = useAuth();
  const [chargers, setChargers] = useState<Charger[]>([]);
  const [sessions, setSessions] = useState<ChargingSession[]>([]);
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const load = useCallback(async () => {
    if (!token || !tenant) return;
    setLoading(true); setError('');
    try {
      const [stationData, sessionData, wallets] = await Promise.all([api.chargers(tenant.id, token), api.sessions(tenant.id, token), api.wallets(tenant.id, token)]);
      setChargers(stationData); setSessions(sessionData.filter(item => !profile || item.userId === profile.id)); setWallet(wallets.find(item => item.userId === profile?.id) || null);
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to load your dashboard.'); }
    finally { setLoading(false); }
  }, [profile, tenant, token]);
  useFocusEffect(useCallback(() => { void load(); }, [load]));
  const active = sessions.filter(item => ['ACTIVE', 'STARTED', 'CHARGING'].includes(item.status));
  const completed = sessions.filter(item => item.status === 'COMPLETED');
  const energy = completed.reduce((sum, item) => sum + Number(item.energyKwh || 0), 0);
  const firstName = profile?.firstName || profile?.fullName?.split(' ')[0] || 'Driver';
  return <Screen refreshing={loading} onRefresh={load}>
    <PageHeader eyebrow={tenant?.name.toUpperCase()} title={`Hello, ${firstName}`} subtitle="Here is what is happening across your charging account."/>
    {error ? <ErrorBanner message={error}/> : null}
    <View style={styles.metrics}><Metric label="WALLET" value={`₹${Number(wallet?.balance || 0).toFixed(0)}`} hint="Available balance"/><Metric label="ACTIVE" value={String(active.length)} hint="Charging sessions"/><Metric label="ENERGY" value={`${energy.toFixed(1)} kWh`} hint="Lifetime delivered"/><Metric label="STATIONS" value={String(chargers.filter(item => item.status === 'AVAILABLE').length)} hint="Available now"/></View>
    <Text style={commonStyles.sectionTitle}>Current charging</Text>
    {active.length ? active.map(session => <Card key={session.id}><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{chargers.find(item => item.id === session.chargerId)?.stationName || chargers.find(item => item.id === session.chargerId)?.stationId || 'Charging station'}</Text><Text style={commonStyles.tiny}>{session.transactionId}</Text></View><Pill label={session.status} tone="green"/></View><View style={commonStyles.divider}/><Text style={styles.energy}>{Number(session.energyKwh || 0).toFixed(2)} kWh</Text><Text style={commonStyles.body}>Session in progress. Open Charging to record the final meter value and stop.</Text></Card>) : <EmptyState title="No active session" message="Choose an available connector from Stations when you are ready to charge."/>}
    <Text style={commonStyles.sectionTitle}>Recently used stations</Text>
    {chargers.slice(0, 3).map(charger => <Card key={charger.id}><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{charger.stationName || charger.stationId}</Text><Text style={commonStyles.tiny}>{[charger.city, charger.state].filter(Boolean).join(', ') || charger.address || 'Location not set'}</Text></View><Pill label={charger.status} tone={charger.status === 'AVAILABLE' ? 'green' : charger.status === 'FAULTED' ? 'red' : 'neutral'}/></View></Card>)}
  </Screen>;
}

const styles = StyleSheet.create({ metrics: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', rowGap: 12 }, flex: { flex: 1 }, energy: { color: colors.blue, fontSize: 24, fontWeight: '900', marginBottom: 5 } });
