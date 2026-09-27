import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Columns3 } from 'lucide-react';

type TableInfo={key:string;name:string;columns:string[]};
const storageKey=(key:string)=>`tekwatt-columns:${key}`;
const essentials=(column:string,index:number)=>index<3||/status|action|amount|total|customer|station|invoice|name|charge time|duration|energy/i.test(column);
function storedColumns(table:TableInfo):string[]{
  try {
    const value=localStorage.getItem(storageKey(table.key));
    if(value){const selected=JSON.parse(value);if(Array.isArray(selected))return selected.filter(item=>table.columns.includes(item));}
  }catch{ /* Browser storage can be disabled. */ }
  return table.columns.filter(essentials);
}
export function ManagedTables({route,children}:{route:string;children:ReactNode}){
  const root=useRef<HTMLDivElement>(null);
  const [tables,setTables]=useState<TableInfo[]>([]);
  const [open,setOpen]=useState(false);
  const [selected,setSelected]=useState<Record<string,string[]>>({});
  useEffect(()=>{
    const host=root.current;if(!host)return;
    const scan=()=>{
      const next=Array.from(host.querySelectorAll<HTMLTableElement>('.responsive-table table')).map((table,index)=>{
        const name=table.closest('.table-card')?.querySelector('h3')?.textContent?.trim()||`Table ${index+1}`;
        const columns=Array.from(table.querySelectorAll('thead tr:first-child th')).map(th=>th.textContent?.trim()||'Column');
        const key=`${route}:${index}:${name}`;
        table.dataset.managedTable=String(index);
        return {key,name,columns};
      }).filter(table=>table.columns.length>0);
      setTables(previous=>JSON.stringify(previous)===JSON.stringify(next)?previous:next);
      setSelected(previous=>{
        const nextSelected=Object.fromEntries(next.map(table=>[table.key,previous[table.key]??storedColumns(table)]));
        return JSON.stringify(previous)===JSON.stringify(nextSelected)?previous:nextSelected;
      });
    };
    scan();const observer=new MutationObserver(scan);observer.observe(host,{childList:true,subtree:true});
    return()=>observer.disconnect();
  },[route]);
  const toggle=(table:TableInfo,column:string)=>{
    const current=selected[table.key]??storedColumns(table);
    if(current.length===1&&current.includes(column))return;
    const next=current.includes(column)?current.filter(item=>item!==column):[...current,column];
    try{localStorage.setItem(storageKey(table.key),JSON.stringify(next));}catch{ /* Keep this session's choice. */ }
    setSelected(previous=>({...previous,[table.key]:next}));
  };
  const rules=tables.flatMap((table,tableIndex)=>table.columns.flatMap((column,columnIndex)=>{
    const selector=`[data-managed-table="${tableIndex}"] :is(th,td):nth-child(${columnIndex+1})`;
    if(!(selected[table.key]??storedColumns(table)).includes(column))return [`${selector}{display:none}`];
    if(/amount|total|energy|meter|price|tax|discount|revenue|balance|power|fee/i.test(column))return [`${selector}{text-align:right;white-space:nowrap}`];
    return [];
  })).join('\n');
  return <div ref={root} className="managed-tables">
    {tables.length>0&&<div className="column-toolbar"><button type="button" className="secondary" aria-expanded={open} onClick={()=>setOpen(!open)}><Columns3 size={15}/> Manage columns</button>
      {open&&<div className="column-panel">{tables.map(table=><fieldset key={table.key}><legend>{table.name}</legend>{table.columns.map(column=><label key={column}><input type="checkbox" checked={(selected[table.key]??storedColumns(table)).includes(column)} onChange={()=>toggle(table,column)}/>{column}</label>)}</fieldset>)}</div>}</div>}
    <style>{rules}</style>{children}
  </div>;
}
