import { useState } from 'react';
import type { Bill, Charger, ChargingSession, Invoice, UserProfile } from './api';
import { api } from './api';
import { InvoiceViewer, printInvoice } from './InvoiceDocuments';

export function InvoiceAdminPage({tenantId,bills,invoices,sessions,chargers,users,refresh}:{tenantId?:string;bills:Bill[];invoices:Invoice[];sessions:ChargingSession[];chargers:Charger[];users:UserProfile[];refresh:()=>Promise<void>}){
  const [billId,setBillId]=useState('');const [viewing,setViewing]=useState<Invoice|null>(null);
  const [busy,setBusy]=useState(false);const [error,setError]=useState('');
  const available=bills.filter(bill=>!invoices.some(invoice=>invoice.billId===bill.id)&&bill.status!=='VOID');
  const create=async()=>{
    const bill=bills.find(item=>item.id===billId),user=users.find(item=>item.id===bill?.userId);
    if(!tenantId||!bill||!user||busy)return;
    setBusy(true);setError('');
    try{
      const created=await api.createInvoice({tenantId,userId:user.id,billId:bill.id,customerName:`${user.firstName??''} ${user.lastName??''}`.trim()||user.email,customerEmail:user.email,subtotal:bill.subtotal,taxAmount:bill.taxAmount,totalAmount:bill.totalAmount,currency:bill.currency,issueDate:new Date().toISOString().slice(0,10),dueDate:new Date(Date.now()+7*86400000).toISOString().slice(0,10)});
      await api.invoiceAction(created.id,'issue');setBillId('');await refresh();
    }catch(reason){setError(reason instanceof Error?reason.message:'Invoice could not be created or issued. Check drafts below before retrying.');await refresh().catch(()=>{});}finally{setBusy(false);}
  };
  const action=async(invoice:Invoice,operation:'issue'|'void')=>{
    if(busy)return;setBusy(true);setError('');
    try{await api.invoiceAction(invoice.id,operation);await refresh();}catch(reason){setError(reason instanceof Error?reason.message:'Invoice could not be updated.');}finally{setBusy(false);}
  };
  return <section>
    <div className="page-title split"><div><span className="eyebrow">PAYMENTS</span><h1>Invoices</h1><p>Create, issue, view and print itemized charging invoices.</p></div><div className="action-group"><select aria-label="Bill to invoice" value={billId} onChange={event=>setBillId(event.target.value)}><option value="">Select uninvoiced bill</option>{available.map(bill=><option key={bill.id} value={bill.id}>{bill.billNumber} · {bill.currency} {Number(bill.totalAmount).toFixed(2)}</option>)}</select><button className="primary" disabled={!billId||busy} onClick={()=>void create()}>{busy?'Working…':'Create & issue invoice'}</button></div></div>
    {error&&<div className="form-error" role="alert">{error}</div>}
    <article className="card table-card"><div className="card-head"><h3>Invoices</h3><span className="record-count">{invoices.length} records</span></div><div className="responsive-table"><table><thead><tr><th>Invoice</th><th>Customer</th><th>Transaction</th><th>Total</th><th>Status</th><th>Actions</th></tr></thead><tbody>{invoices.map(invoice=>{
      const bill=bills.find(item=>item.id===invoice.billId),session=sessions.find(item=>item.id===bill?.sessionId);
      return <tr key={invoice.id}><td><strong>{invoice.invoiceNumber}</strong></td><td>{invoice.customerName}</td><td>{session?.transactionId||'—'}</td><td>{invoice.currency} {Number(invoice.totalAmount).toFixed(2)}</td><td><span className={`status ${invoice.status.toLowerCase()}`}>{invoice.status}</span></td><td><div className="action-group"><button className="table-action" onClick={()=>setViewing(invoice)}>View</button><button className="table-action" onClick={()=>void printInvoice(invoice,bills,sessions,chargers).catch(reason=>setError(reason instanceof Error?reason.message:'Print failed.'))}>Print</button><button className="table-action" disabled={busy||invoice.status!=='DRAFT'} onClick={()=>void action(invoice,'issue')}>Issue</button><button className="table-action danger" disabled={busy||invoice.status==='PAID'||invoice.status==='VOID'} onClick={()=>void action(invoice,'void')}>Void</button></div></td></tr>;
    })}</tbody></table>{invoices.length===0&&<div className="empty-state">No invoices yet. Choose a finalized bill above.</div>}</div></article>
    <InvoiceViewer invoice={viewing} bills={bills} sessions={sessions} chargers={chargers} close={()=>setViewing(null)}/>
  </section>;
}
