import { useCallback, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Card, EmptyState, ErrorBanner, PageHeader, Pill, PrimaryButton, SecondaryButton, Screen, commonStyles } from '../components/ui';
import type { Charger, Reservation } from '../types';

export function ReservationsScreen(){
  const {token,tenant,profile}=useAuth();
  const navigation=useNavigation<any>();
  const [reservations,setReservations]=useState<Reservation[]>([]);const [chargers,setChargers]=useState<Charger[]>([]);const [loading,setLoading]=useState(false);const [working,setWorking]=useState('');const [error,setError]=useState('');
  const load=useCallback(async()=>{if(!token||!tenant)return;setLoading(true);setError('');try{const [items,stationData]=await Promise.all([api.reservations(tenant.id,token),api.chargers(tenant.id,token)]);setReservations(items.filter(item=>!profile||item.userId===profile.id).sort((a,b)=>b.createdAt.localeCompare(a.createdAt)));setChargers(stationData);}catch(reason){setError(reason instanceof Error?reason.message:'Unable to load reservations.');}finally{setLoading(false);}},[profile,tenant,token]);
  useFocusEffect(useCallback(()=>{void load();},[load]));
  const cancel=async(id:string)=>{if(!token)return;setWorking(id);setError('');try{await api.cancelReservation(id,token);await load();}catch(reason){setError(reason instanceof Error?reason.message:'Unable to cancel the reservation.');}finally{setWorking('');}};
  const start=async(item:Reservation)=>{if(!token||!tenant||!profile)return;const charger=chargers.find(row=>row.id===item.chargerId);if(!charger)return setError('The reserved charger is no longer available.');setWorking(`start-${item.id}`);setError('');try{await api.startSession({tenantId:tenant.id,userId:profile.id,chargerId:item.chargerId,connectorId:item.connectorId,transactionId:`APP-${Date.now()}`,meterStartWh:0,pricePerKwh:charger.pricePerKwh||0,currency:'INR'},token);try{await api.completeReservation(item.id,token);}catch{/* Charging has started; the reservation can expire naturally if completion sync fails. */}navigation.navigate('Main',{screen:'Charge'});}catch(reason){setError(reason instanceof Error?reason.message:'Unable to start charging from this reservation.');}finally{setWorking('');}};
  const stationName=(chargerId:string)=>{const charger=chargers.find(item=>item.id===chargerId);return charger?.stationName||charger?.stationId||'Charging station';};
  return <Screen refreshing={loading} onRefresh={load}><PageHeader eyebrow="CHARGING" title="My reservations" subtitle="Review upcoming connector reservations and cancel when plans change."/>{error?<ErrorBanner message={error}/>:null}{reservations.length?reservations.map(item=><Card key={item.id}><View style={commonStyles.between}><View style={styles.flex}><Text style={commonStyles.strong}>{stationName(item.chargerId)}</Text><Text style={commonStyles.tiny}>{item.reference}</Text></View><Pill label={item.status} tone={item.status==='ACTIVE'?'green':item.status==='CANCELLED'?'red':'neutral'}/></View><View style={commonStyles.divider}/><Text style={commonStyles.body}>Starts {new Date(item.startsAt).toLocaleString()}</Text><Text style={commonStyles.body}>Reserved until {new Date(item.expiresAt).toLocaleString()}</Text>{item.status==='ACTIVE'?<View style={styles.action}><View style={styles.flex}><SecondaryButton label="Cancel" disabled={Boolean(working)} onPress={()=>void cancel(item.id)}/></View><View style={styles.flex}><PrimaryButton label="Start charging" compact disabled={Boolean(working)||new Date(item.startsAt)>new Date()} loading={working===`start-${item.id}`} onPress={()=>void start(item)}/></View></View>:null}</Card>):<EmptyState title="No reservations" message="Open Stations and reserve an available connector before arriving."/>}</Screen>;
}

const styles=StyleSheet.create({flex:{flex:1},action:{marginTop:14,flexDirection:'row',gap:8}});
