import { StatusBar } from 'expo-status-bar';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { AuthProvider, useAuth } from './src/auth/AuthContext';
import { AppNavigator } from './src/navigation/AppNavigator';
import { LoginScreen } from './src/screens/LoginScreen';
import { colors } from './src/theme';

function AppContent() {
  const { booting, token } = useAuth();
  if (booting) return <View style={styles.boot}><View style={styles.bolt}><Text style={styles.boltText}>⚡</Text></View><Text style={styles.bootTitle}>TekWatt Nexus</Text><ActivityIndicator color={colors.cyan} style={styles.spinner}/></View>;
  return token ? <AppNavigator/> : <LoginScreen/>;
}

export default function App() {
  return <SafeAreaProvider><AuthProvider><StatusBar style="auto"/><AppContent/></AuthProvider></SafeAreaProvider>;
}

const styles = StyleSheet.create({ boot: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.navy }, bolt: { width: 68, height: 68, borderRadius: 22, backgroundColor: colors.cyan, alignItems: 'center', justifyContent: 'center' }, boltText: { fontSize: 30 }, bootTitle: { color: colors.white, fontSize: 23, fontWeight: '900', marginTop: 15 }, spinner: { marginTop: 20 } });
