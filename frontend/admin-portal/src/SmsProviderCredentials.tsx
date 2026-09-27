import { useEffect, useState } from 'react';
import { api, type SmsProviderCredential } from './api';

export function SmsProviderCredentials({ tenantId }: { tenantId?: string }) {
  const [provider,setProvider]=useState<'MSG91'|'TWILIO'>('MSG91');
  const [configured,setConfigured]=useState<SmsProviderCredential[]>([]);
  const [secret,setSecret]=useState('');
  const [accountSid,setAccountSid]=useState('');
  const [sender,setSender]=useState('');
  const [templateId,setTemplateId]=useState('');
  const [chargingStartedTemplateId,setChargingStartedTemplateId]=useState('');
  const [chargingCompletedTemplateId,setChargingCompletedTemplateId]=useState('');
  const [messageVariable,setMessageVariable]=useState('message');
  const [busy,setBusy]=useState(false);
  const [message,setMessage]=useState('');
  const [loading,setLoading]=useState(false);
  useEffect(()=>{
    if(!tenantId){setConfigured([]);return;}
    let cancelled=false;setLoading(true);setMessage('');
    void api.smsProviderCredentials(tenantId).then(items=>{if(!cancelled)setConfigured(items);})
      .catch(reason=>{if(!cancelled)setMessage(reason instanceof Error?reason.message:'Provider status could not be loaded.');})
      .finally(()=>{if(!cancelled)setLoading(false);});
    return()=>{cancelled=true;};
  },[tenantId]);
  useEffect(()=>{
    const item=configured.find(value=>value.provider===provider);
    setSecret('');setAccountSid(item?.publicIdentifier??'');setSender(item?.sender??'');
    setTemplateId(item?.templateId??'');setMessageVariable(item?.messageVariable||'message');
    setChargingStartedTemplateId(item?.chargingStartedTemplateId??'');
    setChargingCompletedTemplateId(item?.chargingCompletedTemplateId??'');
  },[provider,configured]);
  const existing=configured.find(value=>value.provider===provider);
  const save=async()=>{
    if(!tenantId||busy)return;
    if(provider==='TWILIO'&&!/^AC[0-9a-fA-F]{32}$/.test(accountSid.trim())){
      setMessage('Enter the Twilio Account SID beginning with AC. An API Key SID beginning with SK is not an Account SID.');
      return;
    }
    setBusy(true);setMessage('');
    try{
      const item=await api.saveSmsProviderCredentials(tenantId,{provider,publicIdentifier:accountSid.trim(),sender:sender.trim(),templateId:templateId.trim(),chargingStartedTemplateId:chargingStartedTemplateId.trim(),chargingCompletedTemplateId:chargingCompletedTemplateId.trim(),messageVariable:messageVariable.trim(),secret:secret.trim()});
      setConfigured(values=>[...values.filter(value=>value.provider!==provider),item]);setSecret('');
      setMessage(`${provider} credentials saved for this workspace.`);
    }catch(reason){
      const detail=reason instanceof Error?reason.message:'Provider credentials could not be saved.';
      setMessage(detail.includes('permission')?'Your login is not authorized by the server for provider credentials in this workspace. Ask the platform owner to check your administrator access.':detail);
    }
    finally{setBusy(false);}
  };
  return <div className="sms-credentials full">
    <div className="page-title"><span className="eyebrow">ADMIN ONLY</span><h2>Messaging provider credentials</h2><p>Enter the SMS provider key for this workspace. Existing keys are never displayed again.</p></div>
    <div className="form-grid">
      <label>Provider<select value={provider} onChange={event=>{setProvider(event.target.value as 'MSG91'|'TWILIO');setMessage('');}}><option value="MSG91">MSG91</option><option value="TWILIO">Twilio</option></select></label>
      <div className="provider-status" role="status">{loading?'Checking configuration…':existing?.secretConfigured?'Key configured · leave key blank to keep it':'No key configured'}</div>
      {provider==='MSG91'?<>
        <label>General MSG91 Flow template ID<input value={templateId} onChange={event=>setTemplateId(event.target.value)} /></label>
        <label>Template message variable<input value={messageVariable} onChange={event=>setMessageVariable(event.target.value)} /></label>
        <label>Charging started Flow template ID<input value={chargingStartedTemplateId} onChange={event=>setChargingStartedTemplateId(event.target.value)} placeholder="Approved start template ID" /></label>
        <label>Charging completed Flow template ID<input value={chargingCompletedTemplateId} onChange={event=>setChargingCompletedTemplateId(event.target.value)} placeholder="Approved completion template ID" /></label>
        <p className="full">Use separate approved MSG91 Flow templates for charging start and completion. Leave an event ID blank to keep that event's SMS unavailable.</p>
        <label className="full">MSG91 auth key<input type="password" value={secret} onChange={event=>setSecret(event.target.value)} placeholder={existing?.secretConfigured?'Leave blank to keep saved key':'Enter MSG91 auth key'} autoComplete="new-password" /></label>
      </>:<>
        <label>Twilio account SID (starts AC)<input value={accountSid} onChange={event=>setAccountSid(event.target.value)} /></label>
        <label>Twilio sender number<input value={sender} onChange={event=>setSender(event.target.value)} placeholder="+91…" /></label>
        <label className="full">Twilio auth token<input type="password" value={secret} onChange={event=>setSecret(event.target.value)} placeholder={existing?.secretConfigured?'Leave blank to keep saved token':'Enter Twilio auth token'} autoComplete="new-password" /></label>
      </>}
    </div>
    {message&&<div className={message.includes('saved')?'success-message':'form-error'} role="status">{message}</div>}
    <div className="modal-actions"><button type="button" className="primary" disabled={!tenantId||busy||loading} onClick={()=>void save()}>{busy?'Saving…':'Save provider credentials'}</button></div>
  </div>;
}
