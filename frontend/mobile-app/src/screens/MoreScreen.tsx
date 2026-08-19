import { StyleSheet, Text, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { API_BASE_URL } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, PageHeader, Pill, Screen, SecondaryButton, commonStyles } from '../components/ui';
import { colors } from '../theme';

export function MoreScreen() {
  const { tenant, profile, claims, signOut } = useAuth();
  const navigation = useNavigation<any>();
  const name = profile?.fullName || `${profile?.firstName || ''} ${profile?.lastName || ''}`.trim() || claims?.email || 'TekWatt customer';
  return <Screen>
    <PageHeader eyebrow="ACCOUNT" title="More" subtitle="Profile, support and application connection details."/>
    <Card><View style={styles.avatar}><Text style={styles.avatarText}>{name.split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase()}</Text></View><Text style={styles.name}>{name}</Text><Text style={styles.email}>{profile?.email || claims?.email}</Text><View style={styles.status}><Pill label={profile?.status || 'ACTIVE'} tone="green"/><Pill label="CUSTOMER"/></View></Card>
    <Text style={commonStyles.sectionTitle}>Account details</Text>
    <Card><Row label="Workspace" value={tenant?.name || '—'}/><View style={commonStyles.divider}/><Row label="Phone" value={profile?.phone || 'Not set'}/><View style={commonStyles.divider}/><Row label="City" value={profile?.city || 'Not set'}/><View style={commonStyles.divider}/><Row label="API Gateway" value={API_BASE_URL}/></Card>
    <SecondaryButton label="Open support centre" onPress={() => navigation.navigate('Support')}/>
    <SecondaryButton label="Sign out" onPress={() => void signOut()}/>
    <Text style={styles.version}>TekWatt Nexus Mobile 0.1.0 · OCPP 2.0.1 platform</Text>
  </Screen>;
}

function Row({ label, value }: { label: string; value: string }) { return <View style={commonStyles.between}><Text style={styles.rowLabel}>{label}</Text><Text style={styles.rowValue} numberOfLines={2}>{value}</Text></View>; }
const styles = StyleSheet.create({ avatar: { alignSelf: 'center', width: 64, height: 64, borderRadius: 20, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.cyan }, avatarText: { color: colors.navy, fontSize: 22, fontWeight: '900' }, name: { color: colors.ink, textAlign: 'center', fontSize: 20, fontWeight: '900', marginTop: 12 }, email: { color: colors.muted, textAlign: 'center', fontSize: 13, marginTop: 4 }, status: { flexDirection: 'row', justifyContent: 'center', gap: 7, marginTop: 12 }, rowLabel: { color: colors.muted, fontSize: 12, fontWeight: '700' }, rowValue: { flex: 1, color: colors.ink, fontSize: 12, fontWeight: '800', textAlign: 'right', marginLeft: 20 }, version: { color: colors.muted, fontSize: 10, textAlign: 'center', marginTop: 8 } });
