import { useCallback, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, PageHeader, Pill, Screen, commonStyles } from '../components/ui';
import type { Invoice } from '../types';

export function InvoicesScreen(){
  const {token,tenant,profile}=useAuth();const [items,setItems]=useState<Invoice[]>([]);const [loading,setLoading]=useState(false);const [error,setError]=useState('');
  const load=useCallback(async()=>{if(!token||!tenant)return;setLoading(true);setError('');try{const rows=await api.invoices(tenant.id,token);setItems(rows.filter(item=>!profile||item.userId===profile.id||item.customerEmail.toLowerCase()===profile.email.toLowerCase()).sort((a,b)=>b.createdAt.localeCompare(a.createdAt)));}catch(reason){setError(reason instanceof Error?reason.message:'Unable to load invoices.');}finally{setLoading(false);}},[profile,tenant,token]);
  useFocusEffect(useCallback(()=>{void load();},[load]));
  return <Screen refreshing={loading} onRefresh={load}><PageHeader eyebrow="BILLING" title="Invoices" subtitle="View issued charging invoices, tax and payment status."/>{error?<ErrorBanner message={error}/>:null}{items.length?items.map(item=><Card key={item.id}><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{item.invoiceNumber}</Text><Text style={commonStyles.tiny}>{item.issueDate||new Date(item.createdAt).toLocaleDateString()} · Due {item.dueDate||'not set'}</Text></View><Pill label={item.status} tone={item.status==='PAID'?'green':item.status==='VOID'?'neutral':'amber'}/></View><View style={styles.amountRow}><View><Text style={commonStyles.tiny}>Subtotal</Text><Text style={commonStyles.body}>₹{Number(item.subtotal).toFixed(2)}</Text></View><View><Text style={commonStyles.tiny}>Tax</Text><Text style={commonStyles.body}>₹{Number(item.taxAmount).toFixed(2)}</Text></View><View><Text style={commonStyles.tiny}>Total</Text><Text style={styles.total}>₹{Number(item.totalAmount).toFixed(2)}</Text></View></View></Card>):<EmptyState title="No invoices" message="Issued charging invoices will appear here."/>}</Screen>;
}

const styles=StyleSheet.create({flex:{flex:1},amountRow:{flexDirection:'row',justifyContent:'space-between',marginTop:16},total:{color:'#007FAA',fontSize:17,fontWeight:'900'}});
