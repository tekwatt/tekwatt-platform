import type { PropsWithChildren, ReactNode } from 'react';
import { ActivityIndicator, Pressable, RefreshControl, ScrollView, StyleSheet, Text, TextInput, type TextInputProps, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors, radius, shadow } from '../theme';

export function Screen({ children, refreshing, onRefresh }: PropsWithChildren<{ refreshing?: boolean; onRefresh?: () => void }>) {
  return <SafeAreaView style={styles.safe} edges={['top']}><ScrollView style={styles.scroll} contentContainerStyle={styles.screen} keyboardShouldPersistTaps="handled" refreshControl={onRefresh ? <RefreshControl refreshing={Boolean(refreshing)} onRefresh={onRefresh} tintColor={colors.cyan} colors={[colors.blue]}/> : undefined}>{children}{refreshing && !onRefresh && <View style={styles.loadingOverlay}><ActivityIndicator color={colors.cyan}/></View>}</ScrollView></SafeAreaView>;
}

export function PageHeader({ eyebrow, title, subtitle, action }: { eyebrow?: string; title: string; subtitle?: string; action?: ReactNode }) {
  return <View style={styles.header}><View style={styles.headerCopy}>{eyebrow && <Text style={styles.eyebrow}>{eyebrow}</Text>}<Text style={styles.title}>{title}</Text>{subtitle && <Text style={styles.subtitle}>{subtitle}</Text>}</View>{action}</View>;
}

export function Card({ children, style }: PropsWithChildren<{ style?: object }>) { return <View style={[styles.card, style]}>{children}</View>; }

export function PrimaryButton({ label, onPress, disabled, loading, compact }: { label: string; onPress: () => void; disabled?: boolean; loading?: boolean; compact?: boolean }) {
  return <Pressable accessibilityRole="button" disabled={disabled || loading} onPress={onPress} style={({ pressed }) => [styles.primary, compact && styles.compactButton, (disabled || loading) && styles.disabled, pressed && styles.pressed]}>{loading ? <ActivityIndicator color={colors.white} size="small"/> : <Text style={styles.primaryText}>{label}</Text>}</Pressable>;
}

export function SecondaryButton({ label, onPress, disabled }: { label: string; onPress: () => void; disabled?: boolean }) {
  return <Pressable accessibilityRole="button" disabled={disabled} onPress={onPress} style={({ pressed }) => [styles.secondary, disabled && styles.disabled, pressed && styles.pressed]}><Text style={styles.secondaryText}>{label}</Text></Pressable>;
}

export function Field({ label, error, ...props }: TextInputProps & { label: string; error?: string }) {
  return <View style={styles.field}><Text style={styles.fieldLabel}>{label}</Text><TextInput placeholderTextColor="#8AA0AB" {...props} style={[styles.input, props.multiline && styles.multiline, error && styles.invalid, props.style]}/>{error && <Text style={styles.fieldError}>{error}</Text>}</View>;
}

export function ErrorBanner({ message }: { message: string }) { return <View accessibilityRole="alert" style={styles.error}><View style={styles.errorIcon}><Text style={styles.errorMark}>!</Text></View><View style={styles.errorCopy}><Text style={styles.errorTitle}>Unable to complete this action</Text><Text style={styles.errorMessage}>{message}</Text></View></View>; }
export function EmptyState({ title, message }: { title: string; message: string }) { return <Card style={styles.empty}><Text style={styles.emptyIcon}>⚡</Text><Text style={styles.emptyTitle}>{title}</Text><Text style={styles.emptyMessage}>{message}</Text></Card>; }
export function Pill({ label, tone = 'neutral' }: { label: string; tone?: 'green' | 'amber' | 'red' | 'neutral' }) { return <View style={[styles.pill, styles[`pill_${tone}`]]}><Text style={[styles.pillText, styles[`pillText_${tone}`]]}>{label.replaceAll('_', ' ')}</Text></View>; }
export function Metric({ label, value, hint }: { label: string; value: string; hint: string }) { return <Card style={styles.metric}><Text style={styles.metricLabel}>{label}</Text><Text style={styles.metricValue}>{value}</Text><Text style={styles.metricHint}>{hint}</Text></Card>; }

export const commonStyles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  between: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  sectionTitle: { fontSize: 18, lineHeight: 24, fontWeight: '800', color: colors.ink, marginTop: 10, marginBottom: 10 },
  body: { color: colors.muted, fontSize: 14, lineHeight: 21 },
  strong: { color: colors.ink, fontSize: 15, lineHeight: 21, fontWeight: '800' },
  tiny: { color: colors.muted, fontSize: 11, lineHeight: 16 },
  divider: { height: 1, backgroundColor: colors.line, marginVertical: 14 },
});

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background }, scroll: { flex: 1 }, screen: { padding: 18, paddingBottom: 38, gap: 14 },
  loadingOverlay: { padding: 12 }, header: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between', gap: 12, marginBottom: 4 }, headerCopy: { flex: 1 },
  eyebrow: { color: colors.blue, fontSize: 11, fontWeight: '900', letterSpacing: 1.7, marginBottom: 7 }, title: { color: colors.ink, fontSize: 29, lineHeight: 34, fontWeight: '900', letterSpacing: -0.8 }, subtitle: { color: colors.muted, fontSize: 14, lineHeight: 21, marginTop: 6 },
  card: { backgroundColor: colors.surface, borderWidth: 1, borderColor: colors.line, borderRadius: radius.md, padding: 16, ...shadow },
  primary: { minHeight: 48, paddingHorizontal: 18, borderRadius: radius.md, backgroundColor: colors.blue, alignItems: 'center', justifyContent: 'center' }, compactButton: { minHeight: 38, paddingHorizontal: 14, borderRadius: radius.sm }, primaryText: { color: colors.white, fontSize: 14, fontWeight: '900' },
  secondary: { minHeight: 46, paddingHorizontal: 18, borderRadius: radius.md, backgroundColor: colors.white, borderWidth: 1, borderColor: '#A9CFDE', alignItems: 'center', justifyContent: 'center' }, secondaryText: { color: colors.blue, fontSize: 14, fontWeight: '900' }, disabled: { opacity: 0.48 }, pressed: { transform: [{ scale: 0.985 }] },
  field: { gap: 7 }, fieldLabel: { color: colors.ink, fontSize: 13, fontWeight: '800' }, input: { minHeight: 48, borderWidth: 1, borderColor: '#CFE0E7', backgroundColor: colors.white, color: colors.ink, borderRadius: radius.sm, paddingHorizontal: 14, fontSize: 15 }, multiline: { minHeight: 112, paddingTop: 13, textAlignVertical: 'top' }, invalid: { borderColor: colors.red }, fieldError: { color: colors.red, fontSize: 11 },
  error: { flexDirection: 'row', gap: 11, backgroundColor: colors.redSoft, borderWidth: 1, borderColor: '#F3C7C1', borderRadius: radius.md, padding: 13 }, errorIcon: { width: 31, height: 31, borderRadius: 10, backgroundColor: '#FFE0DB', alignItems: 'center', justifyContent: 'center' }, errorMark: { color: colors.red, fontWeight: '900' }, errorCopy: { flex: 1 }, errorTitle: { color: '#7D1D17', fontWeight: '900', fontSize: 13 }, errorMessage: { color: '#A1443D', fontWeight: '600', fontSize: 12, lineHeight: 18, marginTop: 3 },
  empty: { alignItems: 'center', paddingVertical: 28 }, emptyIcon: { fontSize: 24 }, emptyTitle: { color: colors.ink, fontSize: 16, fontWeight: '900', marginTop: 8 }, emptyMessage: { color: colors.muted, textAlign: 'center', fontSize: 13, lineHeight: 19, marginTop: 5 },
  pill: { alignSelf: 'flex-start', borderRadius: radius.pill, paddingVertical: 5, paddingHorizontal: 9 }, pillText: { textTransform: 'capitalize', fontSize: 10, fontWeight: '900' }, pill_green: { backgroundColor: '#E2F7EF' }, pillText_green: { color: colors.green }, pill_amber: { backgroundColor: '#FFF2DB' }, pillText_amber: { color: colors.amber }, pill_red: { backgroundColor: colors.redSoft }, pillText_red: { color: colors.red }, pill_neutral: { backgroundColor: '#EDF3F6' }, pillText_neutral: { color: colors.muted },
  metric: { width: '47.8%', minHeight: 127 }, metricLabel: { color: colors.blue, fontSize: 10, fontWeight: '900', letterSpacing: 1.2 }, metricValue: { color: colors.ink, fontSize: 25, fontWeight: '900', marginTop: 12 }, metricHint: { color: colors.muted, fontSize: 11, lineHeight: 16, marginTop: 5 },
});
