import { NavigationContainer, DefaultTheme } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { StyleSheet, Text, View } from 'react-native';
import { colors } from '../theme';
import { HomeScreen } from '../screens/HomeScreen';
import { StationsScreen } from '../screens/StationsScreen';
import { ChargingScreen } from '../screens/ChargingScreen';
import { WalletScreen } from '../screens/WalletScreen';
import { MoreScreen } from '../screens/MoreScreen';
import { SupportScreen } from '../screens/SupportScreen';

export type RootStackParamList = { Main: undefined; Support: undefined };
export type MainTabsParamList = { Home: undefined; Stations: undefined; Charge: undefined; Wallet: undefined; More: undefined };

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tabs = createBottomTabNavigator<MainTabsParamList>();
const symbols: Record<keyof MainTabsParamList, string> = { Home: '⌂', Stations: '⌁', Charge: '⚡', Wallet: '₹', More: '•••' };

function TabIcon({ route, focused }: { route: keyof MainTabsParamList; focused: boolean }) { return <View style={[styles.icon, focused && styles.iconActive]}><Text style={[styles.symbol, focused && styles.symbolActive]}>{symbols[route]}</Text></View>; }

function MainTabs() {
  return <Tabs.Navigator screenOptions={({ route }) => ({ headerShown: false, tabBarHideOnKeyboard: true, tabBarActiveTintColor: colors.blue, tabBarInactiveTintColor: '#78909B', tabBarLabelStyle: styles.label, tabBarStyle: styles.tabBar, tabBarIcon: ({ focused }) => <TabIcon route={route.name} focused={focused}/> })}>
    <Tabs.Screen name="Home" component={HomeScreen}/>
    <Tabs.Screen name="Stations" component={StationsScreen}/>
    <Tabs.Screen name="Charge" component={ChargingScreen}/>
    <Tabs.Screen name="Wallet" component={WalletScreen}/>
    <Tabs.Screen name="More" component={MoreScreen}/>
  </Tabs.Navigator>;
}

export function AppNavigator() {
  const theme = { ...DefaultTheme, colors: { ...DefaultTheme.colors, background: colors.background, card: colors.surface, primary: colors.blue, border: colors.line, text: colors.ink, notification: colors.red } };
  return <NavigationContainer theme={theme}><Stack.Navigator screenOptions={{ headerTintColor: colors.ink, headerTitleStyle: { fontWeight: '900' }, headerShadowVisible: false, headerStyle: { backgroundColor: colors.background }, contentStyle: { backgroundColor: colors.background } }}><Stack.Screen name="Main" component={MainTabs} options={{ headerShown: false }}/><Stack.Screen name="Support" component={SupportScreen}/></Stack.Navigator></NavigationContainer>;
}

const styles = StyleSheet.create({ tabBar: { height: 70, paddingTop: 7, paddingBottom: 9, borderTopColor: colors.line, backgroundColor: colors.white }, label: { fontSize: 10, fontWeight: '800' }, icon: { minWidth: 31, height: 27, paddingHorizontal: 6, borderRadius: 10, alignItems: 'center', justifyContent: 'center' }, iconActive: { backgroundColor: colors.surfaceSoft }, symbol: { color: '#78909B', fontSize: 17, fontWeight: '900' }, symbolActive: { color: colors.blue } });
