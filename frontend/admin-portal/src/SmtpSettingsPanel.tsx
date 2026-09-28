import { useEffect, useState, type FormEvent } from 'react';
import { api, type SmtpSettingsSummary } from './api';

type SmtpForm={host:string;port:number;securityMode:'STARTTLS'|'SSL';username:string;password:string;fromEmail:string;replyTo:string};
const emptyForm:SmtpForm={host:'',port:587,securityMode:'STARTTLS',username:'',password:'',fromEmail:'',replyTo:''};

export function SmtpSettingsPanel({tenantId}:{tenantId?:string}) {
  const [form,setForm]=useState<SmtpForm>(emptyForm);
  const [status,setStatus]=useState<SmtpSettingsSummary|null>(null);
  const [message,setMessage]=useState('');
  const [error,setError]=useState('');
  const [busy,setBusy]=useState(false);
  const [testing,setTesting]=useState(false);
  useEffect(()=>{
    if(!tenantId)return;
    let cancelled=false;
    void api.smtpSettings(tenantId).then(result=>{
      if(cancelled)return;
      setStatus(result);
      setForm({host:result.host||'',port:result.port||587,securityMode:result.securityMode==='SSL'?'SSL':'STARTTLS',username:result.username||'',password:'',fromEmail:result.fromEmail||'',replyTo:result.replyTo||''});
    }).catch(reason=>{if(!cancelled)setError(reason instanceof Error?reason.message:'Email settings could not be loaded.');});
    return()=>{cancelled=true;};
  },[tenantId]);

  const save=async(event:FormEvent)=>{
    event.preventDefault();if(!tenantId||busy)return;
    setBusy(true);setError('');setMessage('');
    try{
      const result=await api.saveSmtpSettings(tenantId,{...form,host:form.host.trim(),username:form.username.trim(),fromEmail:form.fromEmail.trim(),replyTo:form.replyTo.trim()});
      setStatus(result);setForm(current=>({...current,password:''}));
      setMessage('Email server settings saved. Send a test email before using password reset.');
    }catch(reason){setError(reason instanceof Error?reason.message:'Email settings could not be saved.');}
    finally{setBusy(false);}
  };
  const test=async()=>{
    if(!tenantId||testing)return;
    setTesting(true);setError('');setMessage('');
    try{await api.testSmtpSettings(tenantId);setMessage('Test email sent to your signed-in administrator address. Check the inbox and spam folder.');}
    catch(reason){setError(reason instanceof Error?reason.message:'Test email could not be sent.');}
    finally{setTesting(false);}
  };

  return <form className="card settings-form" onSubmit={event=>void save(event)}>
    <div className="page-title"><span className="eyebrow">PLATFORM ADMIN ONLY</span><h2>Email delivery &amp; password reset</h2><p>Configure the outgoing SMTP account used to send one-time password-reset codes. The password is encrypted on the server and is never shown again.</p></div>
    <div className="form-grid">
      <label>SMTP server<input required autoComplete="off" value={form.host} placeholder="smtp.example.com" onChange={event=>setForm({...form,host:event.target.value})}/></label>
      <label>Port<input required type="number" min="1" max="65535" value={form.port} onChange={event=>setForm({...form,port:Number(event.target.value)})}/></label>
      <label>Connection security<select value={form.securityMode} onChange={event=>setForm({...form,securityMode:event.target.value as 'STARTTLS'|'SSL'})}><option value="STARTTLS">STARTTLS (usually port 587)</option><option value="SSL">SSL/TLS (usually port 465)</option></select></label>
      <label>SMTP username<input required autoComplete="off" value={form.username} onChange={event=>setForm({...form,username:event.target.value})}/></label>
      <label className="full">SMTP password or app-specific key<input type="password" autoComplete="new-password" value={form.password} required={!status?.passwordConfigured} placeholder={status?.passwordConfigured?'Leave blank to keep the saved password':'Enter provider password or app-specific key'} onChange={event=>setForm({...form,password:event.target.value})}/></label>
      <label>From email<input required type="email" value={form.fromEmail} placeholder="no-reply@tekwatt.in" onChange={event=>setForm({...form,fromEmail:event.target.value})}/></label>
      <label>Reply-to email (optional)<input type="email" value={form.replyTo} onChange={event=>setForm({...form,replyTo:event.target.value})}/></label>
    </div>
    <p className="muted">{status?.configured?`Configured via ${status.source==='DATABASE'?'General Settings':'server environment'} · password ${status.passwordConfigured?'saved':'not set'}`:'No email server configured yet.'} A one-time server encryption key is required before saving credentials. The “Send test email” action sends only to your signed-in admin address.</p>
    {message&&<div className="success-message" role="status">{message}</div>}
    {error&&<div className="form-error" role="alert">{error}</div>}
    <div className="modal-actions"><button className="secondary" type="button" disabled={!status?.configured||testing||busy} onClick={()=>void test()}>{testing?'Sending…':'Send test email'}</button><button className="primary" type="submit" disabled={!tenantId||busy||testing}>{busy?'Saving…':'Save email settings'}</button></div>
  </form>;
}
