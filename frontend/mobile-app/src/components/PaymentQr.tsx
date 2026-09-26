import { useMemo } from 'react';
import { View } from 'react-native';
import qrcode from 'qrcode-generator';

/** Local QR rendering: invoice links are never sent to an image/QR service. */
export function PaymentQr({value}:{value:string}) {
  const rows=useMemo(()=>{
    const qr=qrcode(0,'M');qr.addData(value);qr.make();
    return Array.from({length:qr.getModuleCount()},(_,row)=>Array.from({length:qr.getModuleCount()},(_,col)=>qr.isDark(row,col)));
  },[value]);
  const cell=3;
  return <View accessible accessibilityLabel="Scan QR to open the invoice payment page" style={{alignSelf:'center',backgroundColor:'#fff',padding:cell*4}}>{rows.map((row,r)=><View key={r} style={{flexDirection:'row',height:cell}}>{row.map((dark,c)=><View key={c} style={{width:cell,height:cell,backgroundColor:dark?'#000':'#fff'}}/>)}</View>)}</View>;
}
