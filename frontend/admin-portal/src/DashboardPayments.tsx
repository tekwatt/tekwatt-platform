import type { Invoice, Payment } from './api';

export function DashboardPayments({invoices,payments,open}:{invoices:Invoice[];payments:Payment[];open:()=>void}){
  const currencies=[...new Set([...invoices.map(i=>i.currency),...payments.map(p=>p.currency||'INR')])];
  const currency=currencies.includes('INR')?'INR':currencies[0]||'INR';
  const fmt=(amount:number)=>new Intl.NumberFormat('en-IN',{style:'currency',currency,maximumFractionDigits:2}).format(amount);
  const rows=invoices.filter(i=>i.currency===currency);
  const unpaid=rows.filter(i=>['ISSUED','OVERDUE'].includes(i.status));
  const collected=payments.filter(p=>(p.currency||'INR')===currency&&['SUCCEEDED','COMPLETED','SUCCESS','PAID'].includes(p.status||''));
  const pending=payments.filter(p=>(p.currency||'INR')===currency&&['PENDING','PROCESSING'].includes(p.status||''));
  const items=[
    {label:'Payment due',value:fmt(unpaid.reduce((sum,i)=>sum+Number(i.totalAmount),0)),detail:`${unpaid.length} unpaid invoices`},
    {label:'Collected',value:fmt(collected.reduce((sum,p)=>sum+Number(p.amount||0),0)),detail:`${collected.length} successful payments`},
    {label:'Paid invoices',value:String(rows.filter(i=>i.status==='PAID').length),detail:`${rows.length} invoices total`},
    {label:'Pending payments',value:String(pending.length),detail:'Awaiting confirmation'}
  ];
  return <div className="billing-widgets" aria-label="Billing and payment summary">
    {items.map(item=><button key={item.label} type="button" className="billing-widget" onClick={open}>
      <span>{item.label}</span><strong>{item.value}</strong><small>{item.detail}</small>
    </button>)}
  </div>;
}
