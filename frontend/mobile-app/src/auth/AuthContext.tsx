import * as SecureStore from 'expo-secure-store';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react';
import { ApiError, api } from '../api/client';
import type { RegistrationInput, Tenant, TokenClaims, UserProfile } from '../types';

const ACCESS_KEY = 'tekwatt.access-token';
const REFRESH_KEY = 'tekwatt.refresh-token';
const TENANT_KEY = 'tekwatt.tenant-id';

function decodeClaims(token: string): TokenClaims {
  const payload = token.split('.')[1];
  if (!payload) throw new Error('The access token is invalid.');
  const normalized = payload.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(payload.length / 4) * 4, '=');
  return JSON.parse(globalThis.atob(normalized)) as TokenClaims;
}

type AuthState = {
  booting: boolean;
  token: string | null;
  refreshToken: string | null;
  claims: TokenClaims | null;
  tenant: Tenant | null;
  profile: UserProfile | null;
  signIn: (email: string, password: string) => Promise<void>;
  register: (input: RegistrationInput) => Promise<void>;
  signOut: () => Promise<void>;
  refreshProfile: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

async function saveTokens(accessToken: string, refreshToken: string) {
  await Promise.all([SecureStore.setItemAsync(ACCESS_KEY, accessToken), SecureStore.setItemAsync(REFRESH_KEY, refreshToken)]);
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [booting, setBooting] = useState(true);
  const [token, setToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [claims, setClaims] = useState<TokenClaims | null>(null);
  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);

  const clear = useCallback(async () => {
    await Promise.all([SecureStore.deleteItemAsync(ACCESS_KEY), SecureStore.deleteItemAsync(REFRESH_KEY)]);
    setToken(null); setRefreshToken(null); setClaims(null); setTenant(null); setProfile(null);
  }, []);

  const hydrate = useCallback(async (access: string, refresh: string) => {
    const decoded = decodeClaims(access);
    const tenants = await api.tenants(access);
    if (!tenants.length) throw new ApiError('No active TekWatt workspace is available for this account.', 404);
    const storedTenantId = await SecureStore.getItemAsync(TENANT_KEY);
    const selectedTenant = tenants.find(item => item.id === storedTenantId) || tenants[0]!;
    let selectedProfile: UserProfile | null = null;
    try { selectedProfile = await api.userByAuth(decoded.sub, access); } catch (error) {
      if (!(error instanceof ApiError) || error.status !== 404) throw error;
    }
    await SecureStore.setItemAsync(TENANT_KEY, selectedTenant.id);
    setToken(access); setRefreshToken(refresh); setClaims(decoded); setTenant(selectedTenant); setProfile(selectedProfile);
  }, []);

  useEffect(() => {
    void (async () => {
      try {
        const [access, refresh] = await Promise.all([SecureStore.getItemAsync(ACCESS_KEY), SecureStore.getItemAsync(REFRESH_KEY)]);
        if (access && refresh) await hydrate(access, refresh);
      } catch { await clear(); }
      finally { setBooting(false); }
    })();
  }, [clear, hydrate]);

  const signIn = useCallback(async (email: string, password: string) => {
    const result = await api.login(email.trim().toLowerCase(), password);
    await saveTokens(result.accessToken, result.refreshToken);
    try { await hydrate(result.accessToken, result.refreshToken); } catch (error) { await clear(); throw error; }
  }, [clear, hydrate]);

  const register = useCallback(async (input: RegistrationInput) => {
    const tenantId = process.env.EXPO_PUBLIC_DEFAULT_TENANT_ID;
    if (!tenantId) throw new ApiError('Self-registration is not configured. Ask your TekWatt administrator to create your customer account.', 409);
    const result = await api.registerAuth(input.email.trim().toLowerCase(), input.password);
    const decoded = decodeClaims(result.accessToken);
    await api.createUser(decoded.sub, tenantId, input, result.accessToken);
    await saveTokens(result.accessToken, result.refreshToken);
    try { await hydrate(result.accessToken, result.refreshToken); } catch (error) { await clear(); throw error; }
  }, [clear, hydrate]);

  const signOut = useCallback(async () => {
    try { if (refreshToken) await api.logout(refreshToken); } catch { /* local sign-out still succeeds */ }
    await clear();
  }, [clear, refreshToken]);

  const refreshProfile = useCallback(async () => {
    if (!token || !claims) return;
    setProfile(await api.userByAuth(claims.sub, token));
  }, [claims, token]);

  const value = useMemo(() => ({ booting, token, refreshToken, claims, tenant, profile, signIn, register, signOut, refreshProfile }), [booting, token, refreshToken, claims, tenant, profile, signIn, register, signOut, refreshProfile]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
