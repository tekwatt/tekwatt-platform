import { useCallback, useMemo, useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, Field, PageHeader, Pill, PrimaryButton, Screen, SecondaryButton, commonStyles } from '../components/ui';
import { colors, radius } from '../theme';
import type { Charger, Connector } from '../types';

export function StationsScreen() {
  const { token, tenant, profile } = useAuth();
  const navigation = useNavigation<any>();
  const [chargers, setChargers] = useState<Charger[]>([]);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Charger | null>(null);
  const [connectors, setConnectors] = useState<Connector[]>([]);
  const [loading, setLoading] = useState(false);
  const [starting, setStarting] = useState('');
  const [error, setError] = useState('');
  const load = useCallback(async () => { if (!token || !tenant) return; setLoading(true); setError(''); try { setChargers(await api.chargers(tenant.id, token)); } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to load stations.'); } finally { setLoading(false); } }, [tenant, token]);
  useFocusEffect(useCallback(() => { void load(); }, [load]));
  const filtered = useMemo(() => chargers.filter(item => `${item.stationName} ${item.stationId} ${item.city} ${item.address}`.toLowerCase().includes(query.toLowerCase())), [chargers, query]);
  const open = async (charger: Charger) => { if (!token) return; setSelected(charger); setConnectors([]); setError(''); try { setConnectors(await api.connectors(charger.id, token)); } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to load connectors.'); } };
  const start = async (connector: Connector) => {
    if (!token || !tenant || !profile || !selected) return setError('Your customer profile is not linked to this account. Contact support.');
    setStarting(connector.id); setError('');
    try {
      await api.startSession({ tenantId: tenant.id, userId: profile.id, chargerId: selected.id, connectorId: connector.id, transactionId: `APP-${Date.now()}`, meterStartWh: 0, pricePerKwh: selected.pricePerKwh || 0, currency: 'INR' }, token);
      setSelected(null); navigation.navigate('Charge');
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to start charging.'); }
    finally { setStarting(''); }
  };
  return <Screen refreshing={loading} onRefresh={load}>
    <PageHeader eyebrow="CHARGE POINTS" title="Find a station" subtitle="Search live TekWatt chargers and choose an available connector."/>
    <Field label="Search" value={query} onChangeText={setQuery} placeholder="Station, city or address"/>
    {error && !selected ? <ErrorBanner message={error}/> : null}
    {!filtered.length ? <EmptyState title="No stations found" message="Try a different search or ask your administrator to add stations to this workspace."/> : filtered.map(charger => <Pressable key={charger.id} onPress={() => void open(charger)}><Card><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{charger.stationName || charger.stationId}</Text><Text style={commonStyles.tiny}>{[charger.address, charger.city, charger.state].filter(Boolean).join(', ') || 'Location not set'}</Text></View><Pill label={charger.status} tone={charger.status === 'AVAILABLE' ? 'green' : charger.status === 'FAULTED' ? 'red' : 'neutral'}/></View><View style={styles.specs}><Text style={styles.spec}>⚡ {charger.powerKw || 0} kW</Text><Text style={styles.spec}>₹{Number(charger.pricePerKwh || 0).toFixed(2)}/kWh</Text><Text style={styles.spec}>{charger.openingHours || 'Hours not set'}</Text></View></Card></Pressable>)}
    <Modal animationType="slide" transparent visible={Boolean(selected)} onRequestClose={() => setSelected(null)}><View style={styles.backdrop}><View style={styles.sheet}><View style={commonStyles.between}><View style={styles.flex}><Text style={styles.sheetTitle}>{selected?.stationName || selected?.stationId}</Text><Text style={commonStyles.body}>Select an available connector</Text></View><SecondaryButton label="Close" onPress={() => setSelected(null)}/></View>{error ? <ErrorBanner message={error}/> : null}<View style={styles.connectorList}>{connectors.length ? connectors.map(connector => <Card key={connector.id}><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>Connector {connector.connectorNumber} · {connector.type}</Text><Text style={commonStyles.tiny}>{connector.maxPowerKw} kW · {connector.maxVoltage} V · {connector.maxCurrent} A</Text></View><Pill label={connector.status} tone={connector.status === 'AVAILABLE' ? 'green' : 'neutral'}/></View><View style={styles.action}><PrimaryButton label="Start charging" compact disabled={connector.status !== 'AVAILABLE'} loading={starting === connector.id} onPress={() => void start(connector)}/></View></Card>) : <EmptyState title="No connectors" message="This charger does not have a configured connector."/>}</View></View></View></Modal>
  </Screen>;
}

const styles = StyleSheet.create({ flex: { flex: 1 }, specs: { flexDirection: 'row', flexWrap: 'wrap', gap: 7, marginTop: 14 }, spec: { color: colors.blue, backgroundColor: colors.surfaceSoft, borderRadius: radius.pill, paddingVertical: 5, paddingHorizontal: 9, fontSize: 10, fontWeight: '800' }, backdrop: { flex: 1, justifyContent: 'flex-end', backgroundColor: '#00101880' }, sheet: { maxHeight: '78%', backgroundColor: colors.background, borderTopLeftRadius: 26, borderTopRightRadius: 26, padding: 18, gap: 14 }, sheetTitle: { color: colors.ink, fontSize: 21, fontWeight: '900' }, connectorList: { gap: 11 }, action: { marginTop: 13, alignItems: 'flex-end' } });
