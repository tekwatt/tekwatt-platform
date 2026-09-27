import { useEffect, useState } from 'react';
import { api, type Bill, type Charger, type ChargingSession, type Invoice } from './api';
const escape=(value:unknown)=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]!));
export function invoiceHtml(i:Invoice,bill?:Bill,session?:ChargingSession,charger?:Charger){
  const money=(n:number)=>escape(`${i.currency} ${Number(n).toFixed(2)}`);
  const date=(value?:string)=>value?escape(new Date(value).toLocaleString('en-IN')):'—';
  const seconds=session?.startedAt&&session?.stoppedAt?Math.max(0,Math.round((Date.parse(session.stoppedAt)-Date.parse(session.startedAt))/1000)):null;
  const chargeTime=seconds!=null&&Number.isFinite(seconds)?`${Math.floor(seconds/3600)}h ${Math.floor(seconds%3600/60)}m ${seconds%60}s`:bill?`${bill.durationMinutes} minutes`:undefined;
  const line=(label:string,value:unknown)=>`<div><dt>${escape(label)}</dt><dd>${escape(value??'—')}</dd></div>`;
  const lines=bill?`<tr><td>Energy · ${Number(bill.energyKwh).toFixed(3)} kWh</td><td>${money(bill.energyAmount)}</td></tr><tr><td>Charging time · ${bill.durationMinutes} minutes</td><td>${money(bill.timeAmount)}</td></tr><tr><td>Session fee</td><td>${money(bill.sessionFee)}</td></tr>`:`<tr><td>Charging services</td><td>${money(i.subtotal)}</td></tr>`;
  return `<!doctype html><html><head><meta charset="utf-8"><title>Invoice ${escape(i.invoiceNumber)}</title><style>body{font:14px Arial;color:#172b36;max-width:800px;margin:32px auto;padding:24px}h1{color:#007fa5;margin-bottom:4px}h2{margin:10px 0}p{line-height:1.6}.details{display:grid;grid-template-columns:1fr 1fr;gap:8px 20px;padding:15px;background:#f3f8fa;border-radius:8px}dl{margin:12px 0 24px}dt{color:#5b7280;font-size:11px}dd{margin:3px 0;font-weight:600}table{width:100%;border-collapse:collapse}td,th{text-align:left;padding:12px;border-bottom:1px solid #ddd}td:last-child,th:last-child{text-align:right}footer{margin-top:32px;font-size:11px;color:#555}@media print{body{margin:0}.details{print-color-adjust:exact}}</style></head><body><h1>TekWatt invoice</h1><h2>${escape(i.invoiceNumber)}</h2><p>Status: ${escape(i.status)} · Issued: ${escape(i.issueDate)} · Due: ${escape(i.dueDate)}</p><p><strong>Billed to</strong><br>${escape(i.customerName)}<br>${escape(i.customerEmail)}</p><h3>Charging details</h3><dl class="details">${line('Station',charger?.stationName||charger?.stationId)}${line('Charger',charger?`${charger.vendor} ${charger.model} · ${charger.serialNumber}`:undefined)}${line('Location',charger?.address||charger?.city)}${line('Transaction ID',session?.transactionId)}${line('Charge started',date(session?.startedAt))}${line('Charge ended',date(session?.stoppedAt))}${line('Duration',chargeTime)}${line('Energy charged',bill?`${Number(bill.energyKwh).toFixed(3)} kWh`:session?.energyKwh!=null?`${Number(session.energyKwh).toFixed(3)} kWh`:undefined)}${line('Meter start',session?.meterStartWh!=null?`${session.meterStartWh} Wh`:undefined)}${line('Meter end',session?.meterStopWh!=null?`${session.meterStopWh} Wh`:undefined)}</dl><h3>Charges</h3><table><tr><th>Description</th><th>Amount</th></tr>${lines}<tr><td>Subtotal</td><td>${money(i.subtotal)}</td></tr><tr><td>Tax${bill?` (${escape(bill.taxPercent)}%)`:''}</td><td>${money(i.taxAmount)}</td></tr><tr><th>Total</th><th>${money(i.totalAmount)}</th></tr></table><footer>Invoice ID: ${escape(i.id)} · Bill: ${escape(bill?.billNumber||i.billId)}<br>This document is proof of payment only when its status is PAID.</footer></body></html>`;
}
type InvoiceContext={bill:Bill;session:ChargingSession;charger:Charger};
async function details(invoice:Invoice,bills:Bill[],sessions:ChargingSession[],chargers:Charger[]):Promise<InvoiceContext>{
  const bill=bills.find(item=>item.id===invoice.billId)??await api.bill(invoice.billId);
  if(bill.tenantId!==invoice.tenantId||bill.userId!==invoice.userId)throw new Error('Invoice and bill details do not match.');
  const session=sessions.find(item=>item.id===bill.sessionId)??await api.session(bill.sessionId);
  if(session.userId&&session.userId!==invoice.userId)throw new Error('Invoice and charging session do not match.');
  const charger=chargers.find(item=>item.id===session.chargerId)??await api.charger(session.chargerId);
  if(charger.tenantId!==invoice.tenantId)throw new Error('Invoice and charger workspace do not match.');
  return {bill,session,charger};
}
export async function printInvoice(invoice:Invoice,bills:Bill[],sessions:ChargingSession[],chargers:Charger[]){
  const windowForPrint=window.open('','_blank');
  if(!windowForPrint)throw new Error('Allow pop-ups to print or save this invoice as PDF.');
  windowForPrint.opener=null;windowForPrint.document.write('Loading charging details…');
  try{
    const context=await details(invoice,bills,sessions,chargers);
    windowForPrint.document.open();windowForPrint.document.write(invoiceHtml(invoice,context.bill,context.session,context.charger));windowForPrint.document.close();windowForPrint.focus();windowForPrint.print();
  }catch(error){windowForPrint.close();throw error;}
}
export function InvoiceViewer({invoice,bills,sessions,chargers,close}:{invoice:Invoice|null;bills:Bill[];sessions:ChargingSession[];chargers:Charger[];close:()=>void}){
  const [error,setError]=useState('');
  const [context,setContext]=useState<InvoiceContext|null>(null);
  useEffect(()=>{
    if(!invoice)return;let disposed=false;setContext(null);setError('');
    void details(invoice,bills,sessions,chargers).then(value=>{if(!disposed)setContext(value);}).catch(reason=>{if(!disposed)setError(reason instanceof Error?reason.message:'Charging details are unavailable.');});
    return()=>{disposed=true;};
  },[invoice?.id,bills,sessions,chargers]);
  if(!invoice)return null;
  const html=context?invoiceHtml(invoice,context.bill,context.session,context.charger):'';
  const download=()=>{const url=URL.createObjectURL(new Blob([html],{type:'text/html;charset=utf-8'}));const anchor=document.createElement('a');anchor.href=url;anchor.download=`invoice-${invoice.invoiceNumber.replace(/[^a-z0-9_-]/gi,'_')}.html`;anchor.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
  const print=()=>void printInvoice(invoice,bills,sessions,chargers).catch(reason=>setError(reason instanceof Error?reason.message:'Print failed.'));
  return <div className="modal-backdrop" onMouseDown={event=>{if(event.currentTarget===event.target)close();}}><section className="modal invoice-modal" role="dialog" aria-modal="true" aria-label={`Invoice ${invoice.invoiceNumber}`}><div className="modal-head"><h2>{invoice.invoiceNumber}</h2><button type="button" className="icon" onClick={close} aria-label="Close invoice">×</button></div>{error&&<p role="alert" className="form-error">{error}</p>}{context?<iframe title={`Invoice ${invoice.invoiceNumber}`} sandbox="" srcDoc={html}/>:<p>Loading invoice and charging details…</p>}<div className="modal-actions"><button type="button" className="secondary" disabled={!context} onClick={download}>Download HTML</button><button type="button" className="primary" disabled={!context} onClick={print}>Print / Save PDF</button></div></section></div>;
}
