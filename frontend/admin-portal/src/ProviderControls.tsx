import { useState } from 'react';
import { api } from './api';
import { SmsProviderCredentials } from './SmsProviderCredentials';

export type MapProvider={id:string;name:string;tileUrl:string};
export function mapProviders(settings:Record<string,string>):MapProvider[]{
  try{
    const values=JSON.parse(settings.mapProviders||'[]');
    return Array.isArray(values)?values.filter((value):value is MapProvider=>Boolean(value&&typeof value.id==='string'&&typeof value.name==='string'&&typeof value.tileUrl==='string')):[];
  }catch{return[];}
}
export function tileFor(settings:Record<string,string>){
  if(settings.mapProvider==='MAPTILER'&&settings.maptilerKey){
    return {url:`https://api.maptiler.com/maps/streets-v4/{z}/{x}/{y}.png?key=${encodeURIComponent(settings.maptilerKey)}`,attribution:'&copy; OpenStreetMap contributors &copy; MapTiler'};
  }
  const custom=mapProviders(settings).find(item=>item.id===settings.mapProvider);
  if(custom&&validTileUrl(custom.tileUrl))return {url:custom.tileUrl,attribution:'&copy; OpenStreetMap contributors'};
  return {url:'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',attribution:'&copy; OpenStreetMap contributors'};
}
function validTileUrl(value:string){
  return /^https:\/\/[^\s/?#]+\/[^\s]*\{z\}[^\s]*\{x\}[^\s]*\{y\}[^\s]*$/i.test(value)&&!value.includes('@');
}
export function ProviderControls({settings,tenantId,mapProvider,smsProvider,chooseMap,chooseSms,refresh}:{
  settings:Record<string,string>;tenantId?:string;mapProvider:string;smsProvider:string;
  chooseMap:(id:string)=>void;chooseSms:(id:string)=>void;refresh:()=>Promise<void>
}){
  const [kind,setKind]=useState('');const [name,setName]=useState('');const [tileUrl,setTileUrl]=useState('');const [key,setKey]=useState('');
  const [busy,setBusy]=useState(false);const [message,setMessage]=useState('');
  const [smsBusy,setSmsBusy]=useState(false);const [smsMessage,setSmsMessage]=useState('');
  const custom=mapProviders(settings);
  const twilioAdded=settings.smsProviders?.includes('TWILIO')||smsProvider==='TWILIO';
  const maptilerAdded=settings.mapProviders?.includes('MAPTILER')||mapProvider==='MAPTILER';
  let admin=false;
  try { admin=JSON.parse(sessionStorage.getItem('tekwatt-login-identity')??'null')?.role==='ADMIN'; } catch { /* No active admin identity. */ }
  const add=async()=>{
    if(!tenantId||busy||!kind)return;
    if(kind==='CUSTOM_MAP'&&(!name.trim()||!validTileUrl(tileUrl))){setMessage('Enter a provider name and HTTPS tile URL containing {z}, {x} and {y}.');return;}
    if(kind==='MAPTILER'&&!key.trim()){setMessage('Enter your public MapTiler browser key.');return;}
    setBusy(true);setMessage('');
    try{
      if(kind==='TWILIO'){
        await api.saveGovernanceSettings(tenantId,{smsProviders:JSON.stringify(['MSG91','TWILIO']),smsProvider:'TWILIO'});
        chooseSms('TWILIO');
      }else if(kind==='MAPTILER'){
        await api.saveGovernanceSettings(tenantId,{mapProviders:JSON.stringify([...custom.filter(item=>item.id!=='MAPTILER'),{id:'MAPTILER',name:'MapTiler',tileUrl:''}]),mapProvider:'MAPTILER',maptilerKey:key.trim()});
        chooseMap('MAPTILER');
      }else{
        const id=`CUSTOM_${crypto.randomUUID().replaceAll('-','')}`;
        await api.saveGovernanceSettings(tenantId,{mapProviders:JSON.stringify([...custom,{id,name:name.trim(),tileUrl:tileUrl.trim()}]),mapProvider:id});
        chooseMap(id);
      }
      await refresh();setKind('');setName('');setTileUrl('');setKey('');setMessage('Provider added and selected.');
    }catch(error){setMessage(error instanceof Error?error.message:'Provider could not be saved.');}finally{setBusy(false);}
  };
  const selectSms=async(next:string)=>{
    if(!tenantId||smsBusy||next===smsProvider)return;
    const previous=smsProvider;
    chooseSms(next);setSmsBusy(true);setSmsMessage('Saving SMS provider…');
    try{
      await api.saveGovernanceSettings(tenantId,{smsProvider:next});
      await refresh();
      setSmsMessage(`${next==='MSG91'?'MSG91':'Twilio'} is now the active SMS provider for this workspace.`);
    }catch(error){
      chooseSms(previous);
      setSmsMessage(error instanceof Error?error.message:'SMS provider could not be changed.');
    }finally{setSmsBusy(false);}
  };
  return <>
    <label>Map provider<select value={mapProvider} onChange={event=>chooseMap(event.target.value)}><option value="OPENSTREETMAP">OpenStreetMap</option>{maptilerAdded&&<option value="MAPTILER">MapTiler</option>}{custom.filter(item=>item.id!=='MAPTILER').map(item=><option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
    <label>SMS provider<select value={smsProvider} disabled={!tenantId||smsBusy} onChange={event=>void selectSms(event.target.value)}><option value="MSG91">MSG91</option>{twilioAdded&&<option value="TWILIO">Twilio</option>}</select><small>Changing this selection saves it immediately. Sender credentials stay on the notification server.</small>{smsMessage&&<small role="status" className={smsMessage.includes('active SMS provider')?'success-message':smsMessage.startsWith('Saving')?'':'form-error'}>{smsMessage}</small>}</label>
    <div className="provider-add full"><strong>Add provider</strong><div className="provider-fields"><select aria-label="Provider to add" value={kind} onChange={event=>setKind(event.target.value)}><option value="">Choose provider</option><option value="MAPTILER">MapTiler map</option><option value="CUSTOM_MAP">Custom map tiles</option><option value="TWILIO">Twilio SMS</option></select>{kind==='MAPTILER'&&<input aria-label="MapTiler public key" placeholder="MapTiler public browser key" value={key} onChange={event=>setKey(event.target.value)}/>}{kind==='CUSTOM_MAP'&&<><input aria-label="Map provider name" placeholder="Provider name" value={name} onChange={event=>setName(event.target.value)}/><input aria-label="Map tile URL" placeholder="https://tiles.example.com/{z}/{x}/{y}.png" value={tileUrl} onChange={event=>setTileUrl(event.target.value)}/></>}<button type="button" className="secondary" disabled={!kind||busy} onClick={()=>void add()}>{busy?'Adding…':'Add provider'}</button></div>{kind==='TWILIO'&&<small>Enter the Twilio account SID, sender and auth token in the administrator credentials form below.</small>}{message&&<small role="status" className={message.includes('added')?'success-message':'form-error'}>{message}</small>}</div>
    {admin&&<SmsProviderCredentials tenantId={tenantId}/>}
  </>;
}
