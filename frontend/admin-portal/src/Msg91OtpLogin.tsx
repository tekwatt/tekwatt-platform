import { useState } from 'react';
import { api } from './api';

type WidgetResult = Record<string, unknown> | string;
type WidgetOptions = {
  widgetId: string;
  tokenAuth: string;
  identifier: string;
  success: (result: WidgetResult) => void;
  failure: () => void;
};

declare global {
  interface Window { initSendOTP?: (options: WidgetOptions) => void; }
}

let widgetScript: Promise<void> | undefined;
export function loadWidget() {
  if (window.initSendOTP) return Promise.resolve();
  widgetScript ??= new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://verify.msg91.com/otp-provider.js';
    script.async = true;
    script.onload = () => window.initSendOTP ? resolve() : reject(new Error('OTP widget could not start.'));
    script.onerror = () => { widgetScript = undefined; reject(new Error('OTP widget could not be loaded.')); };
    document.head.appendChild(script);
  });
  return widgetScript;
}

export function accessToken(result: WidgetResult): string {
  if (typeof result === 'string') return result;
  for (const key of ['access-token', 'accessToken', 'token', 'message']) {
    const value = result[key];
    if (typeof value === 'string' && value.length > 16) return value;
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      try { return accessToken(value as Record<string, unknown>); } catch { /* Try another field. */ }
    }
  }
  throw new Error('OTP was accepted, but the provider did not return a verification token.');
}

export function Msg91OtpLogin({ email, onVerified, onStart }: {
  email: string;
  onVerified: (token: string) => Promise<void>;
  onStart?: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const open = async () => {
    setError('');
    onStart?.();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setError('Enter your account email first.'); return;
    }
    setBusy(true);
    try {
      const config = await api.msg91OtpConfig();
      await loadWidget();
      window.initSendOTP?.({
        ...config,
        identifier: email.trim().toLowerCase(),
        success: result => {
          try {
            void onVerified(accessToken(result)).catch(reason =>
              setError(reason instanceof Error ? reason.message : 'OTP sign-in failed.'))
              .finally(() => setBusy(false));
          } catch (reason) {
            setError(reason instanceof Error ? reason.message : 'OTP verification could not be completed.');
            setBusy(false);
          }
        },
        failure: () => { setError('OTP verification was not completed.'); setBusy(false); },
      });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'OTP sign-in is unavailable.');
      setBusy(false);
    }
  };
  return <>
    <button className="secondary wide" type="button" disabled={busy} onClick={() => void open()}>
      {busy ? 'Opening one-time code…' : 'Sign in with email OTP'}
    </button>
    {error && <p role="alert">{error}</p>}
  </>;
}
