import { useEffect, useState } from 'react';
import { api, type SmsProviderCredential, type SmsFlowTemplate } from './api';

export function SmsProviderCredentials({ tenantId }: { tenantId?: string }) {
  const [provider,setProvider]=useState<'MSG91'|'TWILIO'>('MSG91');
  const [configured,setConfigured]=useState<SmsProviderCredential[]>([]);
  const [secret,setSecret]=useState('');
  const [accountSid,setAccountSid]=useState('');
  const [sender,setSender]=useState('');
  const [flowTemplates,setFlowTemplates]=useState<SmsFlowTemplate[]>([]);
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
    setFlowTemplates(item?.flowTemplates??[]);
  },[provider,configured]);
  const existing=configured.find(value=>value.provider===provider);
  const save=async()=>{
    if(!tenantId||busy)return;
    if(provider==='TWILIO'&&!/^AC[0-9a-fA-F]{32}$/.test(accountSid.trim())){
      setMessage('Enter the Twilio Account SID beginning with AC. An API Key SID beginning with SK is not an Account SID.');
      return;
    }
    if(provider==='MSG91' && flowTemplates.some(row=>!row.templateKey.trim()||!row.flowId.trim()||!row.messageVariable.trim())){
      setMessage('Complete the event key, Flow template ID and message variable in every row, or remove empty rows.');
      return;
    }
    setBusy(true);setMessage('');
    try{
      const item=await api.saveSmsProviderCredentials(tenantId,{provider,publicIdentifier:accountSid.trim(),sender:sender.trim(),templateId:'',chargingStartedTemplateId:'',chargingCompletedTemplateId:'',messageVariable:'message',flowTemplates:provider==='MSG91'?flowTemplates.map(row=>({templateKey:row.templateKey.trim(),flowId:row.flowId.trim(),messageVariable:row.messageVariable.trim()})):[],secret:secret.trim()});
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
        <div className="full"><h3>MSG91 Flow templates</h3><p>Add one row per notification event. The event key selects its approved Flow template; an unconfigured event will not be sent.</p></div>
        {flowTemplates.map((row,index)=><div className="full sms-template-row" key={index}>
          <label>Event key<input value={row.templateKey} list="sms-template-events" placeholder="charging-started" onChange={event=>setFlowTemplates(values=>values.map((value,i)=>i===index?{...value,templateKey:event.target.value}:value))} /></label>
          <label>MSG91 Flow template ID<input value={row.flowId} placeholder="Approved Flow ID" onChange={event=>setFlowTemplates(values=>values.map((value,i)=>i===index?{...value,flowId:event.target.value}:value))} /></label>
          <label>Message variable<input value={row.messageVariable} placeholder="message" onChange={event=>setFlowTemplates(values=>values.map((value,i)=>i===index?{...value,messageVariable:event.target.value}:value))} /></label>
          <button type="button" className="secondary" aria-label={`Remove template ${row.templateKey||index+1}`} onClick={()=>setFlowTemplates(values=>values.filter((_,i)=>i!==index))}>Remove</button>
        </div>)}
        <datalist id="sms-template-events"><option value="general"/><option value="charging-started"/><option value="charging-completed"/><option value="charging-payment-due"/></datalist>
        <div className="full"><button type="button" className="secondary" onClick={()=>setFlowTemplates(values=>[...values,{templateKey:'',flowId:'',messageVariable:'message'}])}>+ Add template</button></div>
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
