import assert from 'node:assert/strict';
import {stopCharger} from '../src/stopCharger.ts';
const original=globalThis.setTimeout;globalThis.setTimeout=(fn)=>{queueMicrotask(fn);return 0;};
function fixture({offline=false,rejected=false,complete=true,recover=false}={}){
  const calls=[];let reads=0;let statusRequested=false;
  return {calls,request:async(path,options)=>{
    calls.push({path,options});
    if(path==='/api/v1/charging-sessions/s1')return {id:'s1',chargerId:'c1',transactionId:'TRX-1',status:statusRequested&&recover?'INTERRUPTED':++reads>2&&complete?'COMPLETED':'ACTIVE'};
    if(path==='/api/v1/ocpp/commands/status'){statusRequested=true;return {messageId:'status1'};}
    if(path==='/api/v1/chargers/c1')return {stationId:'STATION-1'};
    if(path==='/api/v1/ocpp/connections')return offline?[]:[{stationId:'STATION-1',connected:true,protocol:'ocpp2.0'}];
    if(path==='/api/v1/ocpp/commands/remote-stop'){assert.equal(JSON.parse(options.body).transactionId,'TRX-1');return {messageId:'m1'};}
    if(path.endsWith('/result'))return {result:rejected?'REJECTED':'ACCEPTED'};
    throw new Error('Unexpected API '+path);
  }};
}
try{
  const success=fixture();const a=stopCharger('s1',success.request);const b=stopCharger('s1',success.request);assert.equal(a,b);assert.equal((await a).status,'COMPLETED');assert.equal(success.calls.filter(c=>c.path.endsWith('/remote-stop')).length,1);assert(!success.calls.some(c=>c.path.endsWith('/stop')));
  await assert.rejects(stopCharger('s1',fixture({offline:true}).request),/offline/);
  await assert.rejects(stopCharger('s1',fixture({rejected:true,complete:false}).request),/did not stop/);
  await assert.rejects(stopCharger('s1',fixture({complete:false}).request),/has not confirmed/);
  const recovered=fixture({complete:false,recover:true});assert.equal((await stopCharger('s1',recovered.request)).status,'INTERRUPTED');assert(recovered.calls.some(c=>c.path.endsWith('/status')));assert(!recovered.calls.some(c=>c.path.endsWith('/stop')));
  console.log('Remote stop tests passed: confirmation, duplicate submit, offline, rejection, timeout; no database-only stop.');
}finally{globalThis.setTimeout=original;}
