import { useEffect, useState } from 'react';
import { api, type VerifiedPhone } from './api';
import { accessToken, loadWidget } from './Msg91OtpLogin';

function validPhone(value: string) {
  return /^\+[1-9]\d{9,14}$/.test(value.replace(/[\s()-]/g, ''));
}

async function verifyPhone(phone: string, onVerified: (token: string) => Promise<void>, done: (error?: string) => void) {
  const config = await api.msg91OtpConfig();
  await loadWidget();
  window.initSendOTP?.({
    ...config,
    identifier: phone.replace(/[^\d]/g, ''),
    success: result => {
      try {
        void onVerified(accessToken(result)).then(() => done()).catch(reason =>
          done(reason instanceof Error ? reason.message : 'Phone verification could not be completed.'));
      } catch (reason) {
        done(reason instanceof Error ? reason.message : 'The OTP provider did not return a verification token.');
      }
    },
    failure: () => done('Phone OTP verification was not completed.'),
  });
}

export function Msg91PhoneLogin({ onVerified, onStart }: {
  onVerified: (phone: string, token: string) => Promise<void>;
  onStart?: () => void;
}) {
  const [phone, setPhone] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const open = async () => {
    setError(''); onStart?.();
    const number = phone.trim();
    if (!validPhone(number)) { setError('Enter your linked phone number with country code, for example +91…'); return; }
    setBusy(true);
    try { await verifyPhone(number, token => onVerified(number, token), message => { setError(message ?? ''); setBusy(false); }); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Phone OTP is unavailable.'); setBusy(false); }
  };
  return <div className="phone-otp-action">
    <label>Linked phone number<input type="tel" autoComplete="tel" placeholder="+91…" value={phone} onChange={event => setPhone(event.target.value)} /></label>
    <button className="secondary wide" type="button" disabled={busy} onClick={() => void open()}>{busy ? 'Opening one-time code…' : 'Sign in with phone OTP'}</button>
    {error && <p role="alert">{error}</p>}
  </div>;
}

export function Msg91PhoneLink() {
  const [current, setCurrent] = useState<VerifiedPhone>();
  const [phone, setPhone] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  useEffect(() => { void api.verifiedPhone().then(setCurrent).catch(reason => setError(reason instanceof Error ? reason.message : 'Phone status could not be loaded.')); }, []);
  const open = async () => {
    setError(''); setMessage('');
    const number = phone.trim();
    if (!validPhone(number)) { setError('Enter the number with country code, for example +91…'); return; }
    setBusy(true);
    try {
      await verifyPhone(number, async token => { const linked = await api.linkVerifiedPhone(number, token); setCurrent(linked); setPhone(''); setMessage('Phone verified and linked to this sign-in account.'); },
        failure => { setError(failure ?? ''); setBusy(false); });
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Phone could not be linked.'); setBusy(false); }
  };
  return <article className="card settings-form">
    <h3>Phone OTP sign-in</h3>
    <p>Verify a phone while signed in to link it to this account. A phone listed on a customer or admin profile alone cannot be used to sign in.</p>
    <p>{current?.phone ? `Verified sign-in phone: ${current.phone}` : 'No verified sign-in phone linked.'}</p>
    <label>Phone number<input type="tel" autoComplete="tel" placeholder="+91…" value={phone} onChange={event => setPhone(event.target.value)} /></label>
    {error && <div className="form-error" role="alert">{error}</div>}
    {message && <div className="success-message" role="status">{message}</div>}
    <div className="modal-actions"><button className="primary" type="button" disabled={busy} onClick={() => void open()}>{busy ? 'Verifying…' : current?.phone ? 'Replace verified phone' : 'Verify and link phone'}</button></div>
  </article>;
}
