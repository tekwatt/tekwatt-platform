import { useEffect, useState, type FormEvent } from 'react';
import { api, type Msg91OtpSettingsSummary } from './api';

export function Msg91OtpSettingsPanel({ tenantId }: { tenantId?: string }) {
  const [widgetId, setWidgetId] = useState('');
  const [tokenAuth, setTokenAuth] = useState('');
  const [serverAuthKey, setServerAuthKey] = useState('');
  const [status, setStatus] = useState<Msg91OtpSettingsSummary | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!tenantId) return;
    let cancelled = false;
    setError('');
    void api.msg91OtpSettings(tenantId).then(result => {
      if (cancelled) return;
      setStatus(result);
      setWidgetId(result.widgetId ?? '');
      setTokenAuth(''); setServerAuthKey('');
    }).catch(reason => { if (!cancelled) setError(reason instanceof Error ? reason.message : 'OTP settings could not be loaded.'); });
    return () => { cancelled = true; };
  }, [tenantId]);

  const save = async (event: FormEvent) => {
    event.preventDefault();
    if (!tenantId || busy) return;
    setBusy(true); setError(''); setNotice('');
    try {
      const result = await api.saveMsg91OtpSettings(tenantId, {
        widgetId: widgetId.trim(), tokenAuth: tokenAuth.trim(), serverAuthKey: serverAuthKey.trim(),
      });
      setStatus(result); setTokenAuth(''); setServerAuthKey('');
      setNotice('OTP settings saved. Test sign-in with an existing account email before relying on it.');
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'OTP settings could not be saved.');
    } finally { setBusy(false); }
  };

  const savedInApp = status?.source === 'DATABASE';
  return <form className="card settings-form" onSubmit={event => void save(event)}>
    <div className="page-title">
      <span className="eyebrow">PLATFORM ADMIN ONLY</span>
      <h2>Login OTP provider</h2>
      <p>Enter the MSG91 widget details and its separate server Authkey. The credentials are encrypted by the authentication service; saved values are never displayed again.</p>
    </div>
    <div className="form-grid">
      <label className="full">MSG91 widget ID
        <input required autoComplete="off" maxLength={128} value={widgetId} onChange={event => setWidgetId(event.target.value)} placeholder="From Client Side Integration" />
      </label>
      <label className="full">Widget tokenAuth
        <input type="password" autoComplete="new-password" required={!savedInApp} maxLength={4096} value={tokenAuth}
          onChange={event => setTokenAuth(event.target.value)} placeholder={savedInApp ? 'Leave blank to keep saved token' : 'From Client Side Integration'} />
      </label>
      <label className="full">Server Authkey
        <input type="password" autoComplete="new-password" required={!savedInApp} maxLength={2048} value={serverAuthKey}
          onChange={event => setServerAuthKey(event.target.value)} placeholder={savedInApp ? 'Leave blank to keep saved Authkey' : 'From Server Side Integration'} />
      </label>
    </div>
    <p className="muted">{status?.configured ? `Configured via ${savedInApp ? 'this app' : 'server environment'}.` : 'Not configured yet.'} An existing SMTP credential encryption key on the auth service is required to save these values. Email OTP requires an MSG91 widget with the email channel enabled.</p>
    {notice && <div className="success-message" role="status">{notice}</div>}
    {error && <div className="form-error" role="alert">{error}</div>}
    <div className="modal-actions"><button className="primary" type="submit" disabled={!tenantId || busy}>{busy ? 'Saving…' : 'Save OTP settings'}</button></div>
  </form>;
}
