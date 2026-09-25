import { useCallback, useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, Field, PageHeader, Pill, PrimaryButton, Screen, commonStyles } from '../components/ui';
import { colors } from '../theme';
import type { Charger, ChargingSession } from '../types';
import { chargingDurationMs, formatChargingDuration, isActiveChargingSession } from '../utils/charging';

export function ChargingScreen() {
  const { token, tenant, profile } = useAuth();
  const [sessions, setSessions] = useState<ChargingSession[]>([]);
  const [chargers, setChargers] = useState<Charger[]>([]);
  const [meterValues, setMeterValues] = useState<Record<string, string>>({});
  const [working, setWorking] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [now, setNow] = useState(Date.now());
  useEffect(() => { const timer = setInterval(() => setNow(Date.now()), 1000); return () => clearInterval(timer); }, []);
  const load = useCallback(async (silent=false) => { if (!token || !tenant) return; if(!silent)setLoading(true); setError(''); try { const [sessionData, chargerData] = await Promise.all([api.sessions(tenant.id, token), api.chargers(tenant.id, token)]); setSessions(sessionData.filter(item => !profile || item.userId === profile.id).sort((a, b) => String(b.startedAt || '').localeCompare(String(a.startedAt || '')))); setChargers(chargerData); } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to load charging sessions.'); } finally { if(!silent)setLoading(false); } }, [profile, tenant, token]);
  useFocusEffect(useCallback(() => { void load();const timer=setInterval(()=>void load(true),10_000);return()=>clearInterval(timer); }, [load]));
  const stop = async (session: ChargingSession) => { if (!token) return; const meterStop = Number(meterValues[session.id]); if (!Number.isFinite(meterStop) || meterStop < Number(session.meterStartWh || 0)) return setError('Enter a final meter value equal to or greater than the starting value.'); setWorking(session.id); setError(''); try { await api.stopSession(session.id, meterStop, token); await load(); } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to stop charging.'); } finally { setWorking(''); } };
  const active = sessions.filter(isActiveChargingSession);
  const history = sessions.filter(item => !active.includes(item));
  const stationName = (id: string) => chargers.find(item => item.id === id)?.stationName || chargers.find(item => item.id === id)?.stationId || 'Charging station';
  return <Screen refreshing={loading} onRefresh={load}>
    <PageHeader eyebrow="CHARGING" title="My sessions" subtitle="Track active charging and review your completed sessions."/>
    {error ? <ErrorBanner message={error}/> : null}
    <Text style={commonStyles.sectionTitle}>Active now</Text>
    {active.length ? active.map(session => { const charger=chargers.find(item=>item.id===session.chargerId); return <Card key={session.id}><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{stationName(session.chargerId)}</Text><Text style={commonStyles.tiny}>{session.transactionId}</Text></View><Pill label={session.status} tone="green"/></View><Text style={styles.started}>Started {session.startedAt ? new Date(session.startedAt).toLocaleString() : 'time unavailable'}</Text><View style={styles.energyRow}><View style={styles.reading}><Text style={styles.value}>{formatChargingDuration(chargingDurationMs(session,now))}</Text><Text style={styles.label}>charging time</Text></View><View style={styles.reading}><Text style={styles.value}>{Number(session.energyKwh || 0).toFixed(2)}</Text><Text style={styles.label}>kWh delivered</Text></View><View style={styles.reading}><Text style={styles.value}>₹{Number(session.totalCost || 0).toFixed(2)}</Text><Text style={styles.label}>estimated cost</Text></View><View style={styles.reading}><Text style={styles.value}>{Number(charger?.powerKw||0).toFixed(0)}</Text><Text style={styles.label}>rated kW</Text></View></View><Text style={styles.remaining}>Time remaining will appear when the charger supplies vehicle battery capacity and state-of-charge.</Text><Field label="Final meter reading (Wh)" value={meterValues[session.id] || ''} onChangeText={value => setMeterValues(current => ({ ...current, [session.id]: value }))} keyboardType="decimal-pad" placeholder={`Minimum ${session.meterStartWh || 0}`}/><PrimaryButton label="Stop charging" loading={working === session.id} onPress={() => void stop(session)}/></Card>; }) : <EmptyState title="Nothing charging" message="Open Stations, choose a charger and select an available connector to begin."/>}
    <Text style={commonStyles.sectionTitle}>History</Text>
    {history.length ? history.map(session => <Card key={session.id}><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{stationName(session.chargerId)}</Text><Text style={commonStyles.tiny}>{session.startedAt ? new Date(session.startedAt).toLocaleString() : session.transactionId}</Text></View><Pill label={session.status} tone={session.status === 'COMPLETED' ? 'green' : 'neutral'}/></View><View style={styles.historyValues}><View><Text style={commonStyles.body}>{formatChargingDuration(chargingDurationMs(session,now))}</Text><Text style={styles.label}>charging time</Text></View><View><Text style={commonStyles.body}>{Number(session.energyKwh || 0).toFixed(2)} kWh</Text><Text style={styles.label}>energy</Text></View><Text style={commonStyles.strong}>₹{Number(session.totalCost || 0).toFixed(2)}</Text></View></Card>) : <EmptyState title="No session history" message="Completed charging sessions will appear here."/>}
  </Screen>;
}

const styles = StyleSheet.create({ flex: { flex: 1 }, started:{color:colors.muted,fontSize:11,marginTop:8}, energyRow: { flexDirection: 'row', flexWrap:'wrap', justifyContent: 'space-between', gap:12, backgroundColor: colors.surfaceSoft, borderRadius: 14, padding: 15, marginVertical: 14 }, reading:{width:'46%'}, value: { color: colors.blue, fontSize: 20, fontWeight: '900' }, label: { color: colors.muted, fontSize: 10, marginTop: 3 }, remaining:{color:colors.muted,fontSize:11,lineHeight:16,marginBottom:12}, historyValues: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 13 } });
