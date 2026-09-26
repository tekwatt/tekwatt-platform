import { useCallback, useState } from 'react';
import { Linking, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { api } from '../api/client';
import { paymentLink } from '../api/paymentLink';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, PageHeader, Pill, Screen, SecondaryButton, commonStyles } from '../components/ui';
import { PaymentQr } from '../components/PaymentQr';
import type { Invoice } from '../types';

export function PaymentInboxScreen(){
  const {token,tenant,profile}=useAuth();
  const [items,setItems]=useState<Invoice[]>([]);const [error,setError]=useState('');const [loading,setLoading]=useState(false);
  const load=useCallback(async()=>{
    if(!token||!tenant||!profile){setItems([]);return;}
    setLoading(true);
    try{const all=await api.invoices(tenant.id,token);setItems(all.filter(i=>i.userId===profile.id&&['ISSUED','OVERDUE','PAID'].includes(i.status)).sort((a,b)=>b.createdAt.localeCompare(a.createdAt)));setError('');}
    catch(e){setError(e instanceof Error?e.message:'Unable to refresh payment inbox.');}finally{setLoading(false);}
  },[token,tenant?.id,profile?.id]);
  useFocusEffect(useCallback(()=>{setItems([]);void load();const timer=setInterval(()=>void load(),15000);return()=>clearInterval(timer);},[load]));
  const open=async(link:string)=>{try{await Linking.openURL(link);}catch{setError('Unable to open the payment page. Please try again.');}};
  return <Screen refreshing={loading} onRefresh={load}><PageHeader eyebrow="NOTIFICATIONS" title="Payment inbox" subtitle="Charging invoices refresh while this screen is open. Open a payment request or scan its QR code."/>{error?<ErrorBanner message={error}/>:null}{items.map(i=>{
    const due=['ISSUED','OVERDUE'].includes(i.status)&&Number(i.totalAmount)>0;const link=paymentLink(i.id,i.tenantId);
    return <Card key={i.id}><View style={commonStyles.between}><Text style={commonStyles.strong}>{due?'Charging payment due':'Payment received'}</Text><Pill label={i.status} tone={due?'amber':'green'}/></View><Text style={commonStyles.body}>Invoice {i.invoiceNumber}</Text><Text style={commonStyles.body}>{i.currency} {Number(i.totalAmount).toFixed(2)} · Tax {Number(i.taxAmount).toFixed(2)}</Text><Text style={commonStyles.tiny}>Issued {i.issueDate} · Due {i.dueDate}</Text>{due&&link?<View style={{gap:12,marginTop:12}}><PaymentQr value={link}/><SecondaryButton label="View invoice & pay" onPress={()=>void open(link)}/><Text style={commonStyles.tiny}>Opens TekWatt in your browser. Sign in with the invoice customer account. Scanning does not pay automatically.</Text></View>:due?<Text style={commonStyles.body}>The customer payment portal URL has not been configured.</Text>:null}</Card>;
  })}{!loading&&!items.length?<EmptyState title="No payment requests" message="Your issued charging invoices will appear here. No push notifications are sent while the app is closed."/>:null}</Screen>;
}
