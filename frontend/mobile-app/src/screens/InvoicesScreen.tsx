import { useCallback, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { api } from '../api/client';
import { payInvoice } from '../api/payInvoice';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, PageHeader, Pill, PrimaryButton, Screen, commonStyles } from '../components/ui';
import type { Invoice } from '../types';

export function InvoicesScreen(){
  const {token,tenant,profile}=useAuth();const [items,setItems]=useState<Invoice[]>([]);const [loading,setLoading]=useState(false);const [error,setError]=useState('');const [notice,setNotice]=useState('');const [payingId,setPayingId]=useState('');
  const load=useCallback(async()=>{if(!token||!tenant)return;setLoading(true);setError('');try{const rows=await api.invoices(tenant.id,token);setItems(rows.filter(item=>!profile||item.userId===profile.id||item.customerEmail.toLowerCase()===profile.email.toLowerCase()).sort((a,b)=>b.createdAt.localeCompare(a.createdAt)));}catch(reason){setError(reason instanceof Error?reason.message:'Unable to load invoices.');}finally{setLoading(false);}},[profile,tenant,token]);
  useFocusEffect(useCallback(()=>{void load();},[load]));
  const pay=async(invoice:Invoice)=>{if(!token||!profile||payingId)return;setPayingId(invoice.id);setError('');setNotice('');try{setNotice(await payInvoice(invoice,profile,token));await load();}catch(reason){setError(reason instanceof Error?reason.message:'Payment could not be completed.');}finally{setPayingId('');}};
  return <Screen refreshing={loading} onRefresh={load}><PageHeader eyebrow="BILLING" title="Invoices" subtitle="View charging invoices and pay securely in the app."/>{error?<ErrorBanner message={error}/>:null}{notice?<Text style={commonStyles.body}>{notice}</Text>:null}{items.length?items.map(item=><Card key={item.id}><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{item.invoiceNumber}</Text><Text style={commonStyles.tiny}>{item.issueDate||new Date(item.createdAt).toLocaleDateString()} · Due {item.dueDate||'not set'}</Text></View><Pill label={item.status} tone={item.status==='PAID'?'green':item.status==='VOID'?'neutral':'amber'}/></View><View style={styles.amountRow}><View><Text style={commonStyles.tiny}>Subtotal</Text><Text style={commonStyles.body}>₹{Number(item.subtotal).toFixed(2)}</Text></View><View><Text style={commonStyles.tiny}>Tax</Text><Text style={commonStyles.body}>₹{Number(item.taxAmount).toFixed(2)}</Text></View><View><Text style={commonStyles.tiny}>Total</Text><Text style={styles.total}>₹{Number(item.totalAmount).toFixed(2)}</Text></View></View>{['ISSUED','OVERDUE'].includes(item.status)&&Number(item.totalAmount)>0?<View style={styles.payButton}><PrimaryButton label="Pay securely in app" loading={payingId===item.id} disabled={Boolean(payingId)} onPress={()=>void pay(item)}/></View>:null}</Card>):<EmptyState title="No invoices" message="Issued charging invoices will appear here."/>}</Screen>;
}

const styles=StyleSheet.create({flex:{flex:1},amountRow:{flexDirection:'row',justifyContent:'space-between',marginTop:16},payButton:{marginTop:14},total:{color:'#007FAA',fontSize:17,fontWeight:'900'}});
