import { useCallback, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, PageHeader, Pill, Screen, commonStyles } from '../components/ui';
import type { RfidCard } from '../types';

export function RfidCardsScreen(){
  const {token,tenant,profile}=useAuth();const [items,setItems]=useState<RfidCard[]>([]);const [loading,setLoading]=useState(false);const [error,setError]=useState('');
  const load=useCallback(async()=>{if(!token||!tenant)return;setLoading(true);setError('');try{const rows=await api.rfidCards(tenant.id,token);setItems(rows.filter(item=>!profile||item.userId===profile.id));}catch(reason){setError(reason instanceof Error?reason.message:'Unable to load RFID cards.');}finally{setLoading(false);}},[profile,tenant,token]);
  useFocusEffect(useCallback(()=>{void load();},[load]));
  return <Screen refreshing={loading} onRefresh={load}><PageHeader eyebrow="ACCESS" title="RFID cards" subtitle="Cards assigned to your charging account."/>{error?<ErrorBanner message={error}/>:null}{items.length?items.map(item=><Card key={item.id}><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{item.label||'TekWatt charging card'}</Text><Text style={styles.uid}>{item.cardUid}</Text><Text style={commonStyles.tiny}>Issued {new Date(item.issuedAt).toLocaleDateString()}</Text></View><Pill label={item.status} tone={item.status==='ACTIVE'?'green':'red'}/></View></Card>):<EmptyState title="No RFID card assigned" message="Ask your TekWatt administrator to issue and assign a card to this account."/>}</Screen>;
}

const styles=StyleSheet.create({flex:{flex:1},uid:{color:'#007FAA',fontSize:16,fontWeight:'900',letterSpacing:1.3,marginVertical:6}});
