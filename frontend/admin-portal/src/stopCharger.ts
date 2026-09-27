type Session = {id:string;chargerId:string;transactionId:string;status:string};
type Client = <T>(path:string, options?:RequestInit)=>Promise<T>;
const pending=new Map<string,Promise<Session>>();
export function stopCharger(id:string,request:Client):Promise<Session>{
  const existing=pending.get(id);if(existing)return existing;
  const work=stop(id,request).finally(()=>pending.delete(id));pending.set(id,work);return work;
}
async function stop(id:string,request:Client):Promise<Session>{
  const session=await request<Session>(`/api/v1/charging-sessions/${id}`);
  if(session.status!=='ACTIVE')return session;
  const charger=await request<{stationId:string}>(`/api/v1/chargers/${session.chargerId}`);
  const connections=await request<{stationId:string;connected:boolean;protocol?:string}[]>('/api/v1/ocpp/connections');
  const connection=connections.find(c=>c.stationId===charger.stationId&&c.connected);
  if(!connection?.protocol)throw new Error('Charger is offline. Reconnect it before requesting stop. The session has not been marked completed.');
  const command=await request<{messageId:string}>('/api/v1/ocpp/commands/remote-stop',{method:'POST',body:JSON.stringify({stationId:charger.stationId,ocppVersion:connection.protocol,transactionId:session.transactionId})});
  for(let attempt=0;attempt<20;attempt++){
    await new Promise(resolve=>setTimeout(resolve,1500));
    const current=await request<Session>(`/api/v1/charging-sessions/${id}`);
    if(current.status!=='ACTIVE')return current;
    const result=await request<{result:string;message?:string}>(`/api/v1/ocpp/commands/${command.messageId}/result`);
    if(['FAILED','REJECTED'].includes(result.result))throw new Error(`Charger did not stop: ${result.message||result.result}. Session remains active.`);
  }
  // Ask for live device evidence, never release a connector on a timer alone.
  await request('/api/v1/ocpp/commands/status',{method:'POST',body:JSON.stringify({stationId:charger.stationId,ocppVersion:connection.protocol})});
  for(let attempt=0;attempt<10;attempt++){
    await new Promise(resolve=>setTimeout(resolve,1500));
    const current=await request<Session>(`/api/v1/charging-sessions/${id}`);
    if(current.status!=='ACTIVE')return current;
  }
  throw new Error('The charger has not confirmed that it stopped. Stop charging at the charger or simulator, unplug the vehicle, then send a fresh Available status. The CMS will release the old session when that report arrives and hold billing until the final meter arrives. Do not start another car while the charger is still charging.');
}
