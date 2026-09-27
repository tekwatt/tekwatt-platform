import { useEffect, useRef, useState } from 'react';
import { importLibrary, setOptions } from '@googlemaps/js-api-loader';

export type GoogleStationPoint = {
  id:string;
  name:string;
  address:string;
  status:string;
  latitude:number;
  longitude:number;
  color:string;
};

let configuredKey = '';

export function GoogleStationMap({apiKey,stations}:{apiKey:string;stations:GoogleStationPoint[]}){
  const container=useRef<HTMLDivElement>(null);
  const [error,setError]=useState('');
  useEffect(()=>{
    if(!container.current||!apiKey||!stations.length)return;
    let cancelled=false;
    let markers:google.maps.Marker[]=[];
    if(configuredKey&&configuredKey!==apiKey){setError('The Google Maps key changed. Reload this page to use the new key.');return;}
    if(!configuredKey){setOptions({key:apiKey,v:'weekly'});configuredKey=apiKey;}
    setError('');
    void (async()=>{
      try{
        const {Map}=await importLibrary('maps');
        await importLibrary('marker');
        if(cancelled||!container.current)return;
        const map=new Map(container.current,{center:{lat:stations[0]!.latitude,lng:stations[0]!.longitude},zoom:12,mapTypeControl:false});
        const bounds=new google.maps.LatLngBounds();
        const info=new google.maps.InfoWindow();
        markers=stations.map(station=>{
          const position={lat:station.latitude,lng:station.longitude};
          bounds.extend(position);
          const marker=new google.maps.Marker({map,position,title:station.name,icon:{path:google.maps.SymbolPath.CIRCLE,scale:11,fillColor:station.color,fillOpacity:1,strokeColor:'#fff',strokeWeight:3}});
          marker.addListener('click',()=>{
            const content=document.createElement('div');content.className='map-popup';
            const title=document.createElement('strong');title.textContent=station.name;content.append(title);
            const address=document.createElement('span');address.textContent=station.address||'Address not provided';content.append(address);
            const status=document.createElement('span');status.textContent=station.status;content.append(status);
            const directions=document.createElement('a');directions.href=`https://www.google.com/maps/dir/?api=1&destination=${station.latitude},${station.longitude}`;directions.target='_blank';directions.rel='noreferrer';directions.textContent='Get directions';content.append(directions);
            info.setContent(content);info.open(map,marker);
          });
          return marker;
        });
        if(stations.length>1)map.fitBounds(bounds,40);
        else map.setZoom(14);
      }catch{if(!cancelled)setError('Google Maps could not load. Check the browser key, billing, Maps JavaScript API and allowed website origins.');}
    })();
    return()=>{cancelled=true;markers.forEach(marker=>marker.setMap(null));};
  },[apiKey,stations]);
  return <><div className="map-canvas" data-testid="station-map"><div ref={container} className="station-map" aria-label="Google map of charging stations"/></div>{error&&<div className="map-notice" role="alert">{error}</div>}</>;
}
