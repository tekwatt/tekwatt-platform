import type { Bill, Charger, ChargingSession, Invoice, Payment } from './api';

export function ChargingTransactions({sessions,bills,invoices,payments,chargers}:{sessions:ChargingSession[];bills:Bill[];invoices:Invoice[];payments:Payment[];chargers:Charger[]}){
  return <article className="card table-card"><div className="card-head"><h3>Charging transactions</h3><span className="record-count">{sessions.length} sessions</span></div>
    <div className="responsive-table"><table><thead><tr><th>Transaction</th><th>Charger</th><th>Energy</th><th>Charge time</th><th>Invoice</th><th>Amount</th><th>Status</th></tr></thead>
      <tbody>{sessions.map(session=>{
        const bill=bills.find(item=>item.sessionId===session.id);
        const invoice=invoices.find(item=>item.billId===bill?.id);
        const charger=chargers.find(item=>item.id===session.chargerId);
        const payment=payments.find(item=>item.invoiceId===invoice?.id&&item.status==='SUCCEEDED');
        const state=invoice?.status==='PAID'||payment?'Paid':invoice?invoice.status==='DRAFT'?'Invoice draft':'Payment due':session.status==='INTERRUPTED'?'Final meter pending':bill?'Invoice pending':session.status==='ACTIVE'?'Charging':'Billing pending';
        return <tr key={session.id}><td><strong>{session.transactionId}</strong><small>{session.startedAt?new Date(session.startedAt).toLocaleString():'—'}</small></td><td>{charger?.stationName||charger?.serialNumber||session.chargerId.slice(0,8)}</td><td>{Number(bill?.energyKwh??session.energyKwh??0).toFixed(3)} kWh</td><td>{bill?`${bill.durationMinutes} min`:'—'}</td><td>{invoice?.invoiceNumber||'—'}</td><td>{bill?`${bill.currency} ${Number(bill.totalAmount).toFixed(2)}`:'—'}</td><td><span className={`status ${state.toLowerCase().replaceAll(' ','-')}`}>{state}</span></td></tr>;
      })}</tbody></table>{sessions.length===0&&<div className="empty-state">No charging transactions yet.</div>}</div>
  </article>;
}
