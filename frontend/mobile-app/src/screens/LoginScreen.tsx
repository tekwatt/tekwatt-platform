import { useState } from 'react';
import { Image, KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '../auth/AuthContext';
import { ErrorBanner, Field, PrimaryButton } from '../components/ui';
import { colors, radius } from '../theme';

export function LoginScreen() {
  const { signIn, register } = useAuth();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [form, setForm] = useState({ email: '', password: '', firstName: '', lastName: '', phone: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const update = (key: keyof typeof form, value: string) => setForm(current => ({ ...current, [key]: value }));

  const submit = async () => {
    setError('');
    if (!form.email.includes('@')) return setError('Enter a valid email address.');
    if (form.password.length < 12) return setError('The password must contain at least 12 characters.');
    if (mode === 'register' && (!form.firstName.trim() || !form.lastName.trim())) return setError('Enter your first and last name.');
    setLoading(true);
    try {
      if (mode === 'login') await signIn(form.email, form.password);
      else await register(form);
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to sign in.'); }
    finally { setLoading(false); }
  };

  return <SafeAreaView style={styles.safe}><KeyboardAvoidingView style={styles.keyboard} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
    <View style={styles.hero}>
      <Image source={require('../../assets/logo-sidebar.webp')} resizeMode="contain" style={styles.logo}/>
      <Text style={styles.kicker}>CHARGE. CONNECT. CONSERVE.</Text>
      <Text style={styles.heroTitle}>Your EV network, in your pocket.</Text>
      <Text style={styles.heroCopy}>Find stations, manage charging and stay in control with TekWatt Nexus.</Text>
    </View>
    <View style={styles.sheet}>
      <View style={styles.switcher}><Pressable onPress={() => { setMode('login'); setError(''); }} style={[styles.switch, mode === 'login' && styles.switchActive]}><Text style={[styles.switchText, mode === 'login' && styles.switchTextActive]}>Sign in</Text></Pressable><Pressable onPress={() => { setMode('register'); setError(''); }} style={[styles.switch, mode === 'register' && styles.switchActive]}><Text style={[styles.switchText, mode === 'register' && styles.switchTextActive]}>Create account</Text></Pressable></View>
      {mode === 'register' && <View style={styles.nameRow}><View style={styles.nameField}><Field label="First name" value={form.firstName} onChangeText={value => update('firstName', value)} autoCapitalize="words"/></View><View style={styles.nameField}><Field label="Last name" value={form.lastName} onChangeText={value => update('lastName', value)} autoCapitalize="words"/></View></View>}
      {mode === 'register' && <Field label="Phone (optional)" value={form.phone} onChangeText={value => update('phone', value)} keyboardType="phone-pad"/>}
      <Field label="Email address" value={form.email} onChangeText={value => update('email', value)} keyboardType="email-address" autoCapitalize="none" autoCorrect={false} placeholder="you@example.com"/>
      <Field label="Password" value={form.password} onChangeText={value => update('password', value)} secureTextEntry autoCapitalize="none"/>
      {error ? <ErrorBanner message={error}/> : null}
      <PrimaryButton label={mode === 'login' ? 'Sign in securely' : 'Create customer account'} onPress={() => void submit()} loading={loading}/>
      <Text style={styles.help}>{mode === 'login' ? 'Use the customer account created by your TekWatt administrator.' : 'Self-registration requires a default workspace ID in the mobile environment.'}</Text>
    </View>
  </KeyboardAvoidingView></SafeAreaView>;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.navy }, keyboard: { flex: 1, justifyContent: 'flex-end' }, hero: { flex: 1, paddingHorizontal: 26, paddingTop: 24, justifyContent: 'center' },
  logo: { width: 205, height: 112, alignSelf: 'center', marginBottom: 18 }, kicker: { color: '#7BDCFF', fontSize: 10, fontWeight: '900', letterSpacing: 1.9 }, heroTitle: { color: colors.white, fontSize: 34, lineHeight: 39, fontWeight: '900', letterSpacing: -1, marginTop: 12 }, heroCopy: { color: '#B9D0DC', fontSize: 14, lineHeight: 21, marginTop: 10 },
  sheet: { backgroundColor: colors.background, borderTopLeftRadius: 28, borderTopRightRadius: 28, padding: 20, gap: 14 }, switcher: { flexDirection: 'row', borderRadius: radius.md, backgroundColor: '#E5EFF3', padding: 4 }, switch: { flex: 1, paddingVertical: 10, alignItems: 'center', borderRadius: 12 }, switchActive: { backgroundColor: colors.white }, switchText: { color: colors.muted, fontSize: 12, fontWeight: '800' }, switchTextActive: { color: colors.blue }, nameRow: { flexDirection: 'row', gap: 10 }, nameField: { flex: 1 }, help: { color: colors.muted, fontSize: 11, textAlign: 'center', lineHeight: 17 },
});
