import { useCallback, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, PageHeader, Pill, SecondaryButton, Screen, commonStyles } from '../components/ui';
import type { UserSession } from '../types';

export function SecurityScreen(){
  const {token}=useAuth();const [items,setItems]=useState<UserSession[]>([]);const [loading,setLoading]=useState(false);const [working,setWorking]=useState('');const [error,setError]=useState('');
  const load=useCallback(async()=>{if(!token)return;setLoading(true);setError('');try{setItems(await api.authSessions(token));}catch(reason){setError(reason instanceof Error?reason.message:'Unable to load signed-in devices.');}finally{setLoading(false);}},[token]);
  useFocusEffect(useCallback(()=>{void load();},[load]));
  const revoke=async(id:string)=>{if(!token)return;setWorking(id);try{await api.revokeSession(id,token);await load();}catch(reason){setError(reason instanceof Error?reason.message:'Unable to sign out this device.');}finally{setWorking('');}};
  const revokeOthers=async()=>{if(!token)return;setWorking('others');try{await api.revokeOtherSessions(token);await load();}catch(reason){setError(reason instanceof Error?reason.message:'Unable to sign out other devices.');}finally{setWorking('');}};
  return <Screen refreshing={loading} onRefresh={load}><PageHeader eyebrow="SECURITY" title="Signed-in devices" subtitle="Review application sessions and remove access from devices you no longer use."/>{error?<ErrorBanner message={error}/>:null}{items.some(item=>!item.current)?<SecondaryButton label={working==='others'?'Signing out…':'Sign out all other devices'} disabled={Boolean(working)} onPress={()=>void revokeOthers()}/>:null}{items.length?items.map(item=><Card key={item.id}><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{item.device||'Unknown device'}</Text><Text style={commonStyles.tiny}>{item.ipAddress||'IP unavailable'} · Last used {item.lastUsedAt?new Date(item.lastUsedAt).toLocaleString():'not recorded'}</Text></View><Pill label={item.current?'THIS DEVICE':'ACTIVE'} tone={item.current?'green':'neutral'}/></View>{!item.current?<View style={styles.action}><SecondaryButton label={working===item.id?'Signing out…':'Sign out device'} disabled={Boolean(working)} onPress={()=>void revoke(item.id)}/></View>:null}</Card>):<EmptyState title="No sessions found" message="Your active sign-in sessions will appear here."/>}</Screen>;
}

const styles=StyleSheet.create({flex:{flex:1},action:{marginTop:14}});
