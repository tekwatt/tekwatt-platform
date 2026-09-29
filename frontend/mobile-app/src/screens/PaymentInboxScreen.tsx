import { useCallback, useRef, useState } from 'react';
import { Linking, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { api } from '../api/client';
import { paymentLink } from '../api/paymentLink';
import { payInvoice } from '../api/payInvoice';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, PageHeader, Pill, PrimaryButton, Screen, SecondaryButton, commonStyles } from '../components/ui';
import { PaymentQr } from '../components/PaymentQr';
import type { Invoice } from '../types';

export function PaymentInboxScreen(){
  const {token,tenant,profile}=useAuth();
  const [items,setItems]=useState<Invoice[]>([]);const [error,setError]=useState('');const [notice,setNotice]=useState('');const [loading,setLoading]=useState(false);const [payingId,setPayingId]=useState('');
  const generation=useRef(0);
  const load=useCallback(async()=>{
    if(!token||!tenant||!profile){setItems([]);return;}
    const current=++generation.current;setLoading(true);
    try{const all=await api.myInvoices(token);if(current!==generation.current)return;setItems(all.filter(i=>['ISSUED','OVERDUE','PAID'].includes(i.status)).sort((a,b)=>b.createdAt.localeCompare(a.createdAt)));setError('');}
    catch(e){if(current===generation.current)setError(e instanceof Error?e.message:'Unable to refresh payment inbox.');}finally{if(current===generation.current)setLoading(false);}
  },[token,tenant?.id,profile?.id]);
  useFocusEffect(useCallback(()=>{setItems([]);void load();const timer=setInterval(()=>void load(),15000);return()=>{generation.current++;clearInterval(timer);};},[load]));
  const open=async(link:string)=>{try{await Linking.openURL(link);}catch{setError('Unable to open the payment page. Please try again.');}};
  const pay=async(invoice:Invoice)=>{if(!token||!profile||payingId)return;setPayingId(invoice.id);setError('');setNotice('');try{setNotice(await payInvoice(invoice,profile,token));await load();}catch(reason){setError(reason instanceof Error?reason.message:'Payment could not be completed.');}finally{setPayingId('');}};
  return <Screen refreshing={loading} onRefresh={load}><PageHeader eyebrow="NOTIFICATIONS" title="Payment inbox" subtitle="Charging invoices refresh while this screen is open. Pay securely in the app or open your invoice in a browser."/>{error?<ErrorBanner message={error}/>:null}{notice?<Text style={commonStyles.body}>{notice}</Text>:null}{items.map(i=>{
    const due=['ISSUED','OVERDUE'].includes(i.status)&&Number(i.totalAmount)>0;const link=paymentLink(i.id,i.tenantId);
    return <Card key={i.id}><View style={commonStyles.between}><Text style={commonStyles.strong}>{due?'Charging payment due':'Payment received'}</Text><Pill label={i.status} tone={due?'amber':'green'}/></View><Text style={commonStyles.body}>Invoice {i.invoiceNumber}</Text><Text style={commonStyles.body}>{i.currency} {Number(i.totalAmount).toFixed(2)} · Tax {Number(i.taxAmount).toFixed(2)}</Text><Text style={commonStyles.tiny}>Issued {i.issueDate} · Due {i.dueDate}</Text>{due?<View style={{gap:12,marginTop:12}}><PrimaryButton label="Pay securely in app" loading={payingId===i.id} disabled={Boolean(payingId)} onPress={()=>void pay(i)}/>{link?<><PaymentQr value={link}/><SecondaryButton label="View invoice in browser" onPress={()=>void open(link)}/><Text style={commonStyles.tiny}>The QR opens the TekWatt payment page. Scanning it does not pay automatically.</Text></>:null}</View>:null}</Card>;
  })}{!loading&&!items.length?<EmptyState title="No payment requests" message="Your issued charging invoices will appear here. No push notifications are sent while the app is closed."/>:null}</Screen>;
}
