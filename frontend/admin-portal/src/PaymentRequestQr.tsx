import { useMemo } from 'react';
import qrcode from 'qrcode-generator';
import type { Invoice } from './api';

export function PaymentRequestQr({invoices}:{invoices:Invoice[]}) {
  const selectedId=new URLSearchParams(window.location.search).get('invoice');
  const selected=invoices.find(i=>i.id===selectedId);
  const due=selected?[selected]:invoices.filter(i=>['ISSUED','OVERDUE'].includes(i.status)&&Number(i.totalAmount)>0).slice(0,1);
  if(selectedId&&!selected)return <div className="mode-banner" role="status">Invoice not found in this account/workspace, or still loading. Sign in as the invoice customer.</div>;
  return <>{due.map(i=><InvoiceQr key={i.id} invoice={i}/>)}</>;
}
function InvoiceQr({invoice}:{invoice:Invoice}) {
  const link=`${window.location.origin}${window.location.pathname}?invoice=${encodeURIComponent(invoice.id)}&workspace=${encodeURIComponent(invoice.tenantId)}#/payments/invoices`;
  const qr=useMemo(()=>{const code=qrcode(0,'M');code.addData(link);code.make();return code.createDataURL(4,16);},[link]);
  const due=['ISSUED','OVERDUE'].includes(invoice.status)&&Number(invoice.totalAmount)>0;
  return <article className="card" style={{padding:20,marginBottom:16}}><h2>Invoice {invoice.invoiceNumber}</h2><p>{invoice.currency} {Number(invoice.totalAmount).toFixed(2)} · Tax {Number(invoice.taxAmount).toFixed(2)} · Due {invoice.dueDate} · {invoice.status}</p>{due&&<><img src={qr} width={220} height={220} alt="Scan to open this TekWatt invoice payment page" style={{maxWidth:'100%',objectFit:'contain'}}/><p>Scan to view this invoice, then choose Pay online below. Customer sign-in is required; this is not a UPI QR.</p><a href={link}>Open invoice payment link</a></>}</article>;
}
