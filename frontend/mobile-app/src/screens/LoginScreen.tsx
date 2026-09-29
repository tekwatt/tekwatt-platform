import { useState } from 'react';
import { Image, KeyboardAvoidingView, Linking, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '../auth/AuthContext';
import { api } from '../api/client';
import { ErrorBanner, Field, PrimaryButton } from '../components/ui';
import { colors, radius } from '../theme';

export function LoginScreen() {
  const { signIn, register } = useAuth();
  const registrationEnabled = Boolean(process.env.EXPO_PUBLIC_DEFAULT_TENANT_ID?.trim());
  const portal = process.env.EXPO_PUBLIC_CUSTOMER_PORTAL_URL?.replace(/\/$/, '');
  const [mode, setMode] = useState<'login' | 'register' | 'request-reset' | 'confirm-reset'>('login');
  const [form, setForm] = useState({ email: '', password: '', firstName: '', lastName: '', phone: '' });
  const [resetCode,setResetCode]=useState('');
  const [notice,setNotice]=useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const update = (key: keyof typeof form, value: string) => setForm(current => ({ ...current, [key]: value }));

  const submit = async () => {
    setError('');
    if (!form.email.includes('@')) return setError('Enter a valid email address.');
    if (mode !== 'request-reset' && form.password.length < 12 && mode !== 'login') return setError('The password must contain at least 12 characters.');
    if (mode === 'register' && (!form.firstName.trim() || !form.lastName.trim())) return setError('Enter your first and last name.');
    setLoading(true);
    try {
      if (mode === 'login') await signIn(form.email, form.password);
      else if(mode==='register') await register(form);
      else if(mode==='request-reset'){
        await api.requestPasswordReset(form.email.trim());
        setNotice('If this account exists, a reset code has been emailed. Check your inbox and spam folder.');
        setMode('confirm-reset');
      } else {
        await api.confirmPasswordReset(form.email.trim(),resetCode.trim(),form.password);
        setNotice('Password updated. Sign in with your new password.');
        setForm(current=>({...current,password:''}));setResetCode('');setMode('login');
      }
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to sign in.'); }
    finally { setLoading(false); }
  };

  return <SafeAreaView style={styles.safe}><KeyboardAvoidingView style={styles.keyboard} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
    <View style={styles.hero}>
      <Image source={require('../../assets/logo-sidebar.webp')} resizeMode="contain" style={styles.logo}/>
      <Text style={styles.kicker}>CHARGE. CONNECT. CONSERVE.</Text>
      <Text style={styles.heroTitle}>Your EV network, in your pocket.</Text>
      <Text style={styles.heroCopy}>Charge as a driver or monitor your stations as a TekWatt CPO.</Text>
    </View>
    <View style={styles.sheet}>
      {registrationEnabled && <View style={styles.switcher}><Pressable onPress={() => { setMode('login'); setError(''); }} style={[styles.switch, mode === 'login' && styles.switchActive]}><Text style={[styles.switchText, mode === 'login' && styles.switchTextActive]}>Sign in</Text></Pressable><Pressable onPress={() => { setMode('register'); setError(''); }} style={[styles.switch, mode === 'register' && styles.switchActive]}><Text style={[styles.switchText, mode === 'register' && styles.switchTextActive]}>Create account</Text></Pressable></View>}
      {mode === 'register' && <View style={styles.nameRow}><View style={styles.nameField}><Field label="First name" value={form.firstName} onChangeText={value => update('firstName', value)} autoCapitalize="words"/></View><View style={styles.nameField}><Field label="Last name" value={form.lastName} onChangeText={value => update('lastName', value)} autoCapitalize="words"/></View></View>}
      {mode === 'register' && <Field label="Phone (optional)" value={form.phone} onChangeText={value => update('phone', value)} keyboardType="phone-pad"/>}
      <Field label="Email address" value={form.email} onChangeText={value => update('email', value)} keyboardType="email-address" autoCapitalize="none" autoCorrect={false} placeholder="you@example.com"/>
      {mode==='confirm-reset'&&<Field label="8-digit email code" value={resetCode} onChangeText={value=>setResetCode(value.replace(/\D/g,'').slice(0,8))} keyboardType="number-pad"/>}
      {mode!=='request-reset'&&<Field label={mode==='confirm-reset'?'New password':'Password'} value={form.password} onChangeText={value => update('password', value)} secureTextEntry autoCapitalize="none"/>}
      {notice?<Text style={styles.help}>{notice}</Text>:null}
      {error ? <ErrorBanner message={error}/> : null}
      <PrimaryButton label={mode==='login'?'Sign in securely':mode==='register'?'Create customer account':mode==='request-reset'?'Email reset code':'Update password'} onPress={() => void submit()} loading={loading}/>
      {mode==='login'?<Pressable onPress={()=>{setMode('request-reset');setError('');setNotice('');}}><Text style={styles.help}>Forgot password?</Text></Pressable>:mode==='request-reset'||mode==='confirm-reset'?<Pressable onPress={()=>{setMode('login');setError('');setNotice('');}}><Text style={styles.help}>Back to sign in</Text></Pressable>:null}
      <Text style={styles.help}>{mode === 'login' ? 'Sign in with your TekWatt customer or CPO account.' : mode==='register'?'Your new customer account will join the TekWatt workspace configured for this app.':' '}</Text>
      {portal ? <Pressable onPress={() => void Linking.openURL(`${portal}/privacy.html`)}><Text style={styles.help}>Privacy policy</Text></Pressable> : null}
    </View>
  </KeyboardAvoidingView></SafeAreaView>;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.navy }, keyboard: { flex: 1, justifyContent: 'flex-end' }, hero: { flex: 1, paddingHorizontal: 26, paddingTop: 24, justifyContent: 'center' },
  logo: { width: 205, height: 112, alignSelf: 'center', marginBottom: 18 }, kicker: { color: '#7BDCFF', fontSize: 10, fontWeight: '900', letterSpacing: 1.9 }, heroTitle: { color: colors.white, fontSize: 34, lineHeight: 39, fontWeight: '900', letterSpacing: -1, marginTop: 12 }, heroCopy: { color: '#B9D0DC', fontSize: 14, lineHeight: 21, marginTop: 10 },
  sheet: { backgroundColor: colors.background, borderTopLeftRadius: 28, borderTopRightRadius: 28, padding: 20, gap: 14 }, switcher: { flexDirection: 'row', borderRadius: radius.md, backgroundColor: '#E5EFF3', padding: 4 }, switch: { flex: 1, paddingVertical: 10, alignItems: 'center', borderRadius: 12 }, switchActive: { backgroundColor: colors.white }, switchText: { color: colors.muted, fontSize: 12, fontWeight: '800' }, switchTextActive: { color: colors.blue }, nameRow: { flexDirection: 'row', gap: 10 }, nameField: { flex: 1 }, help: { color: colors.muted, fontSize: 11, textAlign: 'center', lineHeight: 17 },
});
