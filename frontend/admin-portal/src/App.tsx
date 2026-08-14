import { FormEvent, useEffect, useMemo, useState } from 'react';
import {
  Activity, Bell, Bolt, Building2, ChevronDown, CircleDollarSign,
  CreditCard, FileBarChart, Gauge, Headphones, LayoutDashboard, LogOut,
  Menu, Moon, PlugZap, Plus, Search, Settings, ShieldCheck, Sun,
  Users, WalletCards, X, Zap,
} from 'lucide-react';
import { api, Charger, ChargingSession, Notification, Payment, Report, Tenant, UserProfile } from './api';

type Page = 'Dashboard' | 'Stations' | 'Sessions' | 'Payments' | 'Users' | 'Reports' | 'Support' | 'Settings';
type LiveData = { tenant?: Tenant; chargers: Charger[]; sessions: ChargingSession[]; payments: Payment[]; users: UserProfile[]; reports: Report[]; notifications: Notification[] };

const nav: { label: Page; icon: typeof Gauge; badge?: string }[] = [
  { label: 'Dashboard', icon: LayoutDashboard },
  { label: 'Stations', icon: Building2, badge: '5' },
  { label: 'Sessions', icon: PlugZap },
  { label: 'Payments', icon: WalletCards },
  { label: 'Users', icon: Users },
  { label: 'Reports', icon: FileBarChart },
  { label: 'Support', icon: Headphones },
  { label: 'Settings', icon: Settings },
];

const stations = [
  { name: 'GreenCharge Andheri Hub', city: 'Mumbai', chargers: 4, power: '240 kW', status: 'Online', usage: 82 },
  { name: 'BKC Fast Charge', city: 'Mumbai', chargers: 6, power: '360 kW', status: 'Online', usage: 68 },
  { name: 'TekWatt Station Velachery', city: 'Chennai', chargers: 4, power: '180 kW', status: 'Online', usage: 54 },
  { name: 'TekWatt Station Tambaram', city: 'Chennai', chargers: 3, power: '120 kW', status: 'Maintenance', usage: 31 },
  { name: 'Sandy EV Point', city: 'Bengaluru', chargers: 2, power: '60 kW', status: 'Online', usage: 45 },
];

const sessions = [
  { id: 'SES-20260814-014', station: 'BKC Fast Charge', energy: '31.8 kWh', amount: '₹398.00', status: 'Charging', time: '2 min ago' },
  { id: 'SES-20260814-013', station: 'GreenCharge Andheri Hub', energy: '22.4 kWh', amount: '₹280.00', status: 'Completed', time: '18 min ago' },
  { id: 'SES-20260814-012', station: 'TekWatt Station Velachery', energy: '16.7 kWh', amount: '₹209.00', status: 'Completed', time: '42 min ago' },
  { id: 'SES-20260814-011', station: 'Sandy EV Point', energy: '8.2 kWh', amount: '₹103.00', status: 'Completed', time: '1 hr ago' },
];

function Brand({ compact = false }: { compact?: boolean }) {
  return <div className="brand"><span className="brand-mark"><Zap size={22} fill="currentColor" /></span>{!compact && <span>TekWatt <b>Nexus</b></span>}</div>;
}

function Login({ onLogin, onDemo }: { onLogin: (email: string, password: string, register: boolean) => Promise<void>; onDemo: () => void }) {
  const [email, setEmail] = useState('admin@tekwatt.in');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const submit = async (event: FormEvent, register = false) => {
    event.preventDefault(); setLoading(true); setError('');
    try { await onLogin(email, password, register); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to sign in'); }
    finally { setLoading(false); }
  };
  return <main className="login-page">
    <section className="login-story">
      <Brand />
      <div className="story-content">
        <span className="eyebrow light">EV CHARGING, ORCHESTRATED</span>
        <h1>One network.<br />Infinite momentum.</h1>
        <p>Operate every station, charger and customer journey from one intelligent energy platform.</p>
        <div className="pulse-line"><span /> Network operating normally</div>
      </div>
    </section>
    <section className="login-panel">
      <form className="login-card" onSubmit={submit}>
        <div className="mobile-brand"><Brand /></div>
        <span className="eyebrow">WELCOME BACK</span>
        <h2>Sign in to Nexus</h2>
        <p>Enter your administrator credentials.</p>
        <label>Email address<input type="email" value={email} onChange={e => setEmail(e.target.value)} /></label>
        <label>Password<input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Your backend account password" /></label>
        {error && <div className="form-error">{error}</div>}
        <button className="primary wide" type="submit" disabled={loading}>{loading ? 'Connecting…' : 'Sign in'} <Bolt size={18} /></button>
        <button className="secondary wide" type="button" disabled={loading || password.length < 12} onClick={event => submit(event, true)}>Create first account</button>
        <button className="demo-link" type="button" onClick={onDemo}>Explore with demo data</button>
        <small>Account creation requires a password of at least 12 characters.</small>
      </form>
    </section>
  </main>;
}

function Sidebar({ page, setPage, open, close, chargerCount }: { page: Page; setPage: (page: Page) => void; open: boolean; close: () => void; chargerCount: number }) {
  return <aside className={`sidebar ${open ? 'open' : ''}`}>
    <div className="sidebar-head"><Brand /><button className="icon mobile-only" onClick={close}><X /></button></div>
    <div className="workspace"><span className="avatar mini">TW</span><div><small>WORKSPACE</small><strong>TekWatt India</strong></div><ChevronDown size={16} /></div>
    <nav>{nav.map(item => <button key={item.label} className={page === item.label ? 'active' : ''} onClick={() => { setPage(item.label); close(); }}><item.icon size={19} /><span>{item.label}</span>{item.label === 'Stations' && <em>{chargerCount}</em>}</button>)}</nav>
    <div className="sidebar-foot"><div className="health-dot" /><div><strong>All systems operational</strong><small>Updated just now</small></div></div>
  </aside>;
}

function Header({ page, dark, toggleDark, openMenu, logout, navigate }: { page: Page; dark: boolean; toggleDark: () => void; openMenu: () => void; logout: () => void; navigate: (page: Page) => void }) {
  const search = (value: string) => { const match = nav.find(item => item.label.toLowerCase().includes(value.toLowerCase())); if (match) navigate(match.label); };
  return <header><button className="icon mobile-only" onClick={openMenu}><Menu /></button><div><small>OPERATIONS</small><h2>{page}</h2></div><div className="header-actions"><label className="search"><Search size={17} /><input placeholder="Search sections" onKeyDown={event => { if (event.key === 'Enter') search(event.currentTarget.value); }} /></label><span className="live"><i /> LIVE</span><button className="icon" onClick={toggleDark} aria-label="Toggle theme">{dark ? <Sun /> : <Moon />}</button><button className="icon notification" onClick={() => window.alert('No new platform notifications.')} aria-label="Notifications"><Bell /><i /></button><button className="profile" onClick={() => navigate('Settings')}><span className="avatar">BA</span><span><strong>Bharanidharan</strong><small>System Admin</small></span><ChevronDown size={16} /></button><button className="icon logout" onClick={logout} title="Sign out"><LogOut /></button></div></header>;
}

function Metric({ label, value, trend, icon: Icon, tone }: { label: string; value: string; trend: string; icon: typeof Gauge; tone: string }) {
  return <article className="metric"><div className={`metric-icon ${tone}`}><Icon /></div><div><small>{label}</small><strong>{value}</strong><span>{trend}</span></div></article>;
}

function Dashboard({ setPage, data, demo, onAddStation }: { setPage: (p: Page) => void; data: LiveData; demo: boolean; onAddStation: () => void }) {
  const stationRows = demo ? stations : data.chargers.map((charger, index) => ({ name: charger.stationId, city: `${charger.vendor} ${charger.model}`, chargers: 1, power: charger.protocolVersion, status: charger.status === 'AVAILABLE' || charger.status === 'ONLINE' ? 'Online' : charger.status, usage: 35 + (index * 13) % 55 }));
  const sessionRows = demo ? sessions : data.sessions.map(session => ({ id: session.transactionId || session.id, station: data.chargers.find(charger => charger.id === session.chargerId)?.stationId ?? 'Unknown charger', energy: `${session.energyKwh ?? 0} kWh`, amount: `${session.currency ?? 'INR'} ${session.totalCost ?? 0}`, status: session.status, time: session.startedAt ? new Date(session.startedAt).toLocaleString() : '—' }));
  const online = demo ? 5 : data.chargers.filter(charger => ['AVAILABLE','ONLINE','CHARGING'].includes(charger.status)).length;
  const active = demo ? 18 : data.sessions.filter(session => ['ACTIVE','CHARGING','STARTED'].includes(session.status)).length;
  const energy = demo ? 428.6 : data.sessions.reduce((sum, session) => sum + Number(session.energyKwh ?? 0), 0);
  const revenue = demo ? 12840 : data.payments.filter(payment => ['COMPLETED','SUCCESS','PAID'].includes(payment.status ?? '')).reduce((sum, payment) => sum + Number(payment.amount ?? 0), 0);
  return <>
    <section className="welcome"><div><span className="eyebrow">THURSDAY, 14 AUGUST</span><h1>Good afternoon, Bharanidharan.</h1><p>Your charging network is performing well today.</p></div><button className="primary" onClick={onAddStation}><Plus size={18} /> Add station</button></section>
    <section className="metrics">
      <Metric label="ONLINE CHARGERS" value={`${online} / ${demo ? 5 : data.chargers.length}`} trend={demo ? 'Demo network' : 'Live backend data'} icon={Building2} tone="green" />
      <Metric label="ACTIVE SESSIONS" value={`${active}`} trend={demo ? '+12% from yesterday' : `${data.sessions.length} total sessions`} icon={PlugZap} tone="blue" />
      <Metric label="ENERGY DELIVERED" value={`${energy.toFixed(1)} kWh`} trend={demo ? '+8.4% this week' : 'From recorded sessions'} icon={Bolt} tone="amber" />
      <Metric label="PAYMENTS" value={`₹${revenue.toLocaleString('en-IN')}`} trend={demo ? '+16.2% this week' : `${data.payments.length} records`} icon={CircleDollarSign} tone="violet" />
    </section>
    <section className="dashboard-grid">
      <article className="card performance"><div className="card-head"><div><small>NETWORK PERFORMANCE</small><h3>Energy delivery</h3></div><select defaultValue="7"><option value="7">Last 7 days</option><option value="30">Last 30 days</option></select></div><div className="chart-meta"><strong>2,864.8 <small>kWh</small></strong><span>↑ 11.4%</span></div><div className="bars">{[42,58,48,72,66,86,78].map((h, i) => <div key={i}><span style={{height:`${h}%`}} /><small>{['Fri','Sat','Sun','Mon','Tue','Wed','Thu'][i]}</small></div>)}</div></article>
      <article className="card network"><div className="card-head"><div><small>LIVE NETWORK</small><h3>Station availability</h3></div><button className="link" onClick={() => setPage('Stations')}>View all</button></div><div className="donut"><div><strong>95%</strong><small>Available</small></div></div><div className="legend"><span><i className="online" /> Online <b>19</b></span><span><i className="busy" /> In use <b>7</b></span><span><i className="offline" /> Offline <b>1</b></span></div></article>
    </section>
    <section className="dashboard-grid lower"><StationCard compact setPage={setPage} items={stationRows} /><SessionsCard compact setPage={setPage} items={sessionRows} /></section>
  </>;
}

function StationCard({ compact = false, setPage, items = stations }: { compact?: boolean; setPage?: (p: Page) => void; items?: typeof stations }) {
  const list = compact ? items.slice(0, 4) : items;
  return <article className="card table-card"><div className="card-head"><div><small>INFRASTRUCTURE</small><h3>Station health</h3></div>{compact && <button className="link" onClick={() => setPage?.('Stations')}>View all</button>}</div><div className="station-list">{list.map(s => <div className="station-row" key={s.name}><div className="station-symbol"><Bolt size={18} /></div><div className="station-name"><strong>{s.name}</strong><small>{s.city} · {s.chargers} chargers</small></div><div className="usage"><span style={{width:`${s.usage}%`}} /></div><span className={`status ${s.status.toLowerCase()}`}>{s.status}</span></div>)}</div></article>;
}

function SessionsCard({ compact = false, setPage, items = sessions }: { compact?: boolean; setPage?: (p: Page) => void; items?: typeof sessions }) {
  return <article className="card table-card"><div className="card-head"><div><small>LIVE ACTIVITY</small><h3>Recent sessions</h3></div>{compact && <button className="link" onClick={() => setPage?.('Sessions')}>View all</button>}</div><div className="responsive-table"><table><thead><tr><th>Session</th><th>Station</th><th>Energy</th><th>Amount</th><th>Status</th></tr></thead><tbody>{items.map(s => <tr key={s.id}><td><strong>{s.id}</strong><small>{s.time}</small></td><td>{s.station}</td><td>{s.energy}</td><td>{s.amount}</td><td><span className={`status ${s.status.toLowerCase()}`}>{s.status}</span></td></tr>)}</tbody></table>{items.length === 0 && <div className="empty-state">No records found in the backend.</div>}</div></article>;
}

function PaymentsPage({ data, refresh }: { data: LiveData; refresh: () => Promise<void> }) {
  const [message, setMessage] = useState('');
  const refund = async (payment: Payment) => { setMessage(''); try { await api.refundPayment(payment.id); await refresh(); } catch (reason) { setMessage(reason instanceof Error ? reason.message : 'Refund failed'); } };
  return <section><div className="page-title"><span className="eyebrow">BILLING</span><h1>Payment operations</h1><p>Track collections and initiate supported refunds.</p></div>{message && <div className="form-error">{message}</div>}<article className="card table-card"><div className="card-head"><h3>Payment records</h3><span className="record-count">{data.payments.length} records</span></div><div className="responsive-table"><table><thead><tr><th>ID</th><th>Provider</th><th>Amount</th><th>Status</th><th>Created</th><th>Action</th></tr></thead><tbody>{data.payments.map(payment => <tr key={payment.id}><td><strong>{payment.id.slice(0, 8)}</strong></td><td>{(payment as Payment & { provider?: string }).provider ?? '—'}</td><td>{payment.currency ?? 'INR'} {payment.amount ?? 0}</td><td><span className={`status ${(payment.status ?? '').toLowerCase()}`}>{payment.status}</span></td><td>{payment.createdAt ? new Date(payment.createdAt).toLocaleString() : '—'}</td><td><button className="table-action" disabled={payment.status !== 'SUCCEEDED'} onClick={() => refund(payment)}>Refund</button></td></tr>)}</tbody></table>{!data.payments.length && <div className="empty-state">No payments have been recorded.</div>}</div></article></section>;
}

function UsersPage({ data, refresh }: { data: LiveData; refresh: () => Promise<void> }) {
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', phone: '', password: '' });
  const [error, setError] = useState('');
  const submit = async (event: FormEvent) => { event.preventDefault(); if (!data.tenant) return; setError(''); try { const tokens = await api.register(form.email, form.password); const payload = JSON.parse(atob(tokens.accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))); await api.createUser({ authUserId: payload.sub, tenantId: data.tenant.id, firstName: form.firstName, lastName: form.lastName, email: form.email, phone: form.phone }); setShowForm(false); await refresh(); } catch (reason) { setError(reason instanceof Error ? reason.message : 'User could not be created'); } };
  const deactivate = async (id: string) => { try { await api.deactivateUser(id); await refresh(); } catch (reason) { setError(reason instanceof Error ? reason.message : 'User could not be deactivated'); } };
  return <section><div className="page-title split"><div><span className="eyebrow">IDENTITY</span><h1>Driver community</h1><p>Manage customer profiles and access.</p></div><button className="primary" onClick={() => setShowForm(!showForm)}><Plus size={18} /> Add user</button></div>{showForm && <form className="card inline-form" onSubmit={submit}><div className="form-grid"><label>First name<input required value={form.firstName} onChange={e => setForm({...form,firstName:e.target.value})} /></label><label>Last name<input required value={form.lastName} onChange={e => setForm({...form,lastName:e.target.value})} /></label><label>Email<input required type="email" value={form.email} onChange={e => setForm({...form,email:e.target.value})} /></label><label>Phone<input value={form.phone} onChange={e => setForm({...form,phone:e.target.value})} /></label><label className="full">Temporary password<input required minLength={12} type="password" value={form.password} onChange={e => setForm({...form,password:e.target.value})} /></label></div>{error && <div className="form-error">{error}</div>}<div className="modal-actions"><button className="secondary" type="button" onClick={() => setShowForm(false)}>Cancel</button><button className="primary" type="submit">Create user</button></div></form>}<article className="card table-card"><div className="responsive-table"><table><thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Status</th><th>Action</th></tr></thead><tbody>{data.users.map(user => <tr key={user.id}><td><strong>{user.firstName} {user.lastName}</strong></td><td>{user.email}</td><td>{(user as UserProfile & {phone?:string}).phone ?? '—'}</td><td><span className={`status ${(user.status ?? '').toLowerCase()}`}>{user.status}</span></td><td><button className="table-action danger" disabled={user.status === 'INACTIVE'} onClick={() => deactivate(user.id)}>Deactivate</button></td></tr>)}</tbody></table>{!data.users.length && <div className="empty-state">No customer profiles exist yet.</div>}</div></article></section>;
}

function ReportsPage({ data, refresh }: { data: LiveData; refresh: () => Promise<void> }) {
  const [format, setFormat] = useState('PDF'); const [type, setType] = useState('OVERVIEW'); const [error, setError] = useState('');
  const generate = async () => { if (!data.tenant) return; setError(''); const to = new Date(); const from = new Date(to.getTime() - 30 * 86400000); try { await api.createReport({ tenantId:data.tenant.id, reportType:type, format, from:from.toISOString(), to:to.toISOString() }); await refresh(); } catch(reason){setError(reason instanceof Error?reason.message:'Report generation failed');} };
  return <section><div className="page-title split"><div><span className="eyebrow">ANALYTICS</span><h1>Reports</h1><p>Generate and download operational reports for the last 30 days.</p></div><div className="action-group"><select value={type} onChange={e=>setType(e.target.value)}><option value="OVERVIEW">Overview</option><option value="DAILY">Daily</option></select><select value={format} onChange={e=>setFormat(e.target.value)}><option>PDF</option><option>HTML</option><option>CSV</option></select><button className="primary" onClick={generate}><Plus size={18}/> Generate</button></div></div>{error&&<div className="form-error">{error}</div>}<article className="card table-card"><div className="responsive-table"><table><thead><tr><th>Report</th><th>Format</th><th>Status</th><th>Created</th><th>Action</th></tr></thead><tbody>{data.reports.map(report=><tr key={report.id}><td><strong>{report.type ?? (report as Report & {reportType?:string}).reportType}</strong></td><td>{(report as Report & {format?:string}).format}</td><td><span className={`status ${(report.status??'').toLowerCase()}`}>{report.status}</span></td><td>{report.createdAt?new Date(report.createdAt).toLocaleString():'—'}</td><td><button className="table-action" disabled={report.status!=='COMPLETED'} onClick={()=>api.downloadReport(report.id,report.fileName)}>Download</button></td></tr>)}</tbody></table>{!data.reports.length&&<div className="empty-state">No reports generated yet.</div>}</div></article></section>;
}

function SupportPage({ data, refresh }: { data: LiveData; refresh: () => Promise<void> }) {
  const [form,setForm]=useState({channel:'EMAIL',recipient:'',subject:'',body:''}); const [error,setError]=useState('');
  const create=async(event:FormEvent)=>{event.preventDefault();if(!data.tenant)return;setError('');try{const item=await api.createNotification({tenantId:data.tenant.id,idempotencyKey:crypto.randomUUID(),...form,maxAttempts:3});await api.sendNotification(item.id);setForm({channel:'EMAIL',recipient:'',subject:'',body:''});await refresh();}catch(reason){setError(reason instanceof Error?reason.message:'Notification failed');}};
  return <section><div className="page-title"><span className="eyebrow">COMMUNICATIONS</span><h1>Support notifications</h1><p>Queue customer email, SMS, or push notifications.</p></div><form className="card inline-form" onSubmit={create}><div className="form-grid"><label>Channel<select value={form.channel} onChange={e=>setForm({...form,channel:e.target.value})}><option>EMAIL</option><option>SMS</option><option>PUSH</option></select></label><label>Recipient<input required value={form.recipient} onChange={e=>setForm({...form,recipient:e.target.value})}/></label><label className="full">Subject<input value={form.subject} onChange={e=>setForm({...form,subject:e.target.value})}/></label><label className="full">Message<textarea required value={form.body} onChange={e=>setForm({...form,body:e.target.value})}/></label></div>{error&&<div className="form-error">{error}</div>}<div className="modal-actions"><button className="primary" type="submit">Queue & send</button></div></form><article className="card table-card"><div className="responsive-table"><table><thead><tr><th>Channel</th><th>Recipient</th><th>Subject</th><th>Status</th></tr></thead><tbody>{data.notifications.map(item=><tr key={item.id}><td>{item.channel}</td><td>{item.recipient}</td><td>{item.subject??'—'}</td><td><span className={`status ${item.status.toLowerCase()}`}>{item.status}</span></td></tr>)}</tbody></table>{!data.notifications.length&&<div className="empty-state">No notifications sent yet.</div>}</div></article></section>;
}

function SettingsPage({ data, refresh }: { data: LiveData; refresh: () => Promise<void> }) {
  const tenant=data.tenant; const [form,setForm]=useState({name:tenant?.name??'',slug:(tenant as Tenant & {slug?:string})?.slug??'',contactEmail:(tenant as Tenant & {contactEmail?:string})?.contactEmail??''}); const [message,setMessage]=useState('');
  useEffect(()=>{setForm({name:tenant?.name??'',slug:(tenant as Tenant & {slug?:string})?.slug??'',contactEmail:(tenant as Tenant & {contactEmail?:string})?.contactEmail??''});},[tenant]);
  const save=async(event:FormEvent)=>{event.preventDefault();if(!tenant)return;try{await api.updateTenant(tenant.id,form);setMessage('Settings saved.');await refresh();}catch(reason){setMessage(reason instanceof Error?reason.message:'Save failed');}};
  return <section><div className="page-title"><span className="eyebrow">CONFIGURATION</span><h1>Workspace settings</h1><p>Update the current tenant profile.</p></div><form className="card settings-form" onSubmit={save}><div className="form-grid"><label>Workspace name<input required value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></label><label>Slug<input required pattern="[a-z0-9]+(?:-[a-z0-9]+)*" value={form.slug} onChange={e=>setForm({...form,slug:e.target.value})}/></label><label className="full">Contact email<input required type="email" value={form.contactEmail} onChange={e=>setForm({...form,contactEmail:e.target.value})}/></label></div>{message&&<div className={message==='Settings saved.'?'success-message':'form-error'}>{message}</div>}<div className="modal-actions"><button className="primary" type="submit">Save settings</button></div></form></section>;
}

function GenericPage({ page, data }: { page: Page; data: LiveData }) {
  const content: Record<Exclude<Page, 'Dashboard' | 'Stations' | 'Sessions'>, { icon: typeof Gauge; title: string; copy: string; stats: string[] }> = {
    Payments: { icon: CreditCard, title: 'Payment operations', copy: 'Track collections, settlements, refunds and payment gateway health.', stats: [`${data.payments.length} payment records`, `${data.payments.filter(p => ['COMPLETED','SUCCESS','PAID'].includes(p.status ?? '')).length} completed`, `${data.payments.filter(p => ['PENDING','CREATED'].includes(p.status ?? '')).length} pending`] },
    Users: { icon: Users, title: 'Driver community', copy: 'Manage customers, partners, fleet accounts and access controls.', stats: [`${data.users.length} customer profiles`, `${data.users.filter(u => u.status === 'ACTIVE').length} active`, data.tenant?.name ?? 'Current tenant'] },
    Reports: { icon: FileBarChart, title: 'Reports & analytics', copy: 'Turn charging, revenue and energy data into operational insight.', stats: [`${data.reports.length} generated reports`, `${data.reports.filter(r => r.status === 'COMPLETED').length} ready`, `${data.reports.filter(r => r.status === 'PENDING').length} pending`] },
    Support: { icon: Headphones, title: 'Reviews & support', copy: 'Resolve customer queries and monitor service satisfaction.', stats: ['6 open tickets', '2.4 hr response', '4.8 / 5 rating'] },
    Settings: { icon: Settings, title: 'Platform settings', copy: 'Configure organization, tariffs, notifications and integrations.', stats: ['Organization', 'Tariffs', 'Integrations'] },
  };
  const item = content[page as keyof typeof content];
  return <section><div className="page-title"><span className="eyebrow">TEKWATT NEXUS</span><h1>{item.title}</h1><p>{item.copy}</p></div><div className="feature-grid">{item.stats.map((stat, i) => <article className="card feature" key={stat}><span><item.icon /></span><small>{['OVERVIEW','PERFORMANCE','STATUS'][i]}</small><strong>{stat}</strong></article>)}</div></section>;
}

function AddStationModal({ tenant, close, saved }: { tenant?: Tenant; close: () => void; saved: () => Promise<void> }) {
  const [form, setForm] = useState({ stationId: '', serialNumber: '', vendor: '', model: '', protocolVersion: 'OCPP_2_0_1' });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const update = (name: string, value: string) => setForm(current => ({ ...current, [name]: value }));
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!tenant) { setError('A tenant must be loaded before adding a station.'); return; }
    setSaving(true); setError('');
    try { await api.createCharger({ tenantId: tenant.id, ...form }); await saved(); close(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Station could not be created'); }
    finally { setSaving(false); }
  };
  return <div className="modal-backdrop" onMouseDown={event => { if (event.currentTarget === event.target) close(); }}><form className="modal" onSubmit={submit}><div className="modal-head"><div><span className="eyebrow">CHARGER SERVICE</span><h2>Add charging station</h2></div><button type="button" className="icon" onClick={close}><X /></button></div><div className="form-grid"><label>Station ID<input required value={form.stationId} onChange={e => update('stationId', e.target.value)} placeholder="TW-CHN-001" /></label><label>Serial number<input required value={form.serialNumber} onChange={e => update('serialNumber', e.target.value)} placeholder="SN-2026-0001" /></label><label>Vendor<input required value={form.vendor} onChange={e => update('vendor', e.target.value)} placeholder="TekWatt" /></label><label>Model<input required value={form.model} onChange={e => update('model', e.target.value)} placeholder="Nexus DC 120" /></label><label className="full">OCPP version<select value={form.protocolVersion} onChange={e => update('protocolVersion', e.target.value)}><option value="OCPP_2_0_1">OCPP 2.0.1</option><option value="OCPP_1_6J">OCPP 1.6J</option></select></label></div>{error && <div className="form-error">{error}</div>}<div className="modal-actions"><button type="button" className="secondary" onClick={close}>Cancel</button><button type="submit" className="primary" disabled={saving}>{saving ? 'Creating…' : 'Create station'}</button></div></form></div>;
}

function Portal({ logout, demo }: { logout: () => void; demo: boolean }) {
  const [page, setPage] = useState<Page>('Dashboard');
  const [dark, setDark] = useState(false);
  const [menu, setMenu] = useState(false);
  const [showAddStation, setShowAddStation] = useState(false);
  const [data, setData] = useState<LiveData>({ chargers: [], sessions: [], payments: [], users: [], reports: [], notifications: [] });
  const [loading, setLoading] = useState(!demo);
  const [loadError, setLoadError] = useState('');
  const refresh = async () => {
    if (demo) return;
    setLoading(true); setLoadError('');
    try {
      const tenants = await api.tenants();
      if (!tenants.length) throw new Error('No tenant exists yet. Create a tenant in Swagger before loading operational data.');
      const tenant = tenants[0];
      const results = await Promise.allSettled([
        api.chargers(tenant.id), api.sessions(tenant.id), api.payments(tenant.id), api.users(tenant.id), api.reports(tenant.id), api.notifications(tenant.id),
      ]);
      const failed = results.filter(result => result.status === 'rejected').length;
      const value = <T,>(index: number) => results[index].status === 'fulfilled' ? results[index].value as T[] : [];
      setData({ tenant, chargers: value<Charger>(0), sessions: value<ChargingSession>(1), payments: value<Payment>(2), users: value<UserProfile>(3), reports: value<Report>(4), notifications: value<Notification>(5) });
      if (failed) setLoadError(`${failed} service${failed > 1 ? 's are' : ' is'} temporarily unavailable. Available data is still shown.`);
    } catch (reason) { setLoadError(reason instanceof Error ? reason.message : 'Backend data could not be loaded'); }
    finally { setLoading(false); }
  };
  useEffect(() => { void refresh(); }, [demo]);
  const stationRows = demo ? stations : data.chargers.map((charger, index) => ({ name: charger.stationId, city: `${charger.vendor} ${charger.model}`, chargers: 1, power: charger.protocolVersion, status: charger.status === 'AVAILABLE' || charger.status === 'ONLINE' ? 'Online' : charger.status, usage: 35 + (index * 13) % 55 }));
  const sessionRows = demo ? sessions : data.sessions.map(session => ({ id: session.transactionId || session.id, station: data.chargers.find(charger => charger.id === session.chargerId)?.stationId ?? 'Unknown charger', energy: `${session.energyKwh ?? 0} kWh`, amount: `${session.currency ?? 'INR'} ${session.totalCost ?? 0}`, status: session.status, time: session.startedAt ? new Date(session.startedAt).toLocaleString() : '—' }));
  const body = useMemo(() => page === 'Dashboard' ? <Dashboard setPage={setPage} data={data} demo={demo} onAddStation={() => setShowAddStation(true)} /> : page === 'Stations' ? <><div className="page-title split"><div><span className="eyebrow">INFRASTRUCTURE</span><h1>Charging stations</h1><p>Monitor availability and utilization across the network.</p></div><button className="primary" onClick={() => setShowAddStation(true)}><Plus size={18} /> Add station</button></div><StationCard items={stationRows} /></> : page === 'Sessions' ? <><div className="page-title"><span className="eyebrow">CHARGING ACTIVITY</span><h1>Charging sessions</h1><p>Follow live and completed vehicle charging activity.</p></div><SessionsCard items={sessionRows} /></> : page === 'Payments' ? <PaymentsPage data={data} refresh={refresh} /> : page === 'Users' ? <UsersPage data={data} refresh={refresh} /> : page === 'Reports' ? <ReportsPage data={data} refresh={refresh} /> : page === 'Support' ? <SupportPage data={data} refresh={refresh} /> : <SettingsPage data={data} refresh={refresh} />, [page, data, demo]);
  return <div className={`app ${dark ? 'dark' : ''}`}><Sidebar page={page} setPage={setPage} open={menu} close={() => setMenu(false)} chargerCount={demo ? 5 : data.chargers.length} />{menu && <div className="scrim" onClick={() => setMenu(false)} />}<div className="shell"><Header page={page} dark={dark} toggleDark={() => setDark(!dark)} openMenu={() => setMenu(true)} logout={logout} navigate={setPage} /><main className="content">{demo && <div className="mode-banner">Demo data mode <button onClick={logout}>Connect backend</button></div>}{loading && <div className="loading-bar">Loading data from API Gateway…</div>}{loadError && <div className="api-error"><span>{loadError}</span><button onClick={refresh}>Retry</button></div>}{!loading && body}</main></div>{showAddStation && <AddStationModal tenant={data.tenant} close={() => setShowAddStation(false)} saved={refresh} />}</div>;
}

export default function App() {
  const [authenticated, setAuthenticated] = useState(() => sessionStorage.getItem('tekwatt-demo-auth') === 'true');
  const [demo, setDemo] = useState(() => sessionStorage.getItem('tekwatt-data-mode') === 'demo');
  const login = async (email: string, password: string, register: boolean) => {
    const tokens = register ? await api.register(email, password) : await api.login(email, password);
    sessionStorage.setItem('tekwatt-access-token', tokens.accessToken);
    sessionStorage.setItem('tekwatt-refresh-token', tokens.refreshToken);
    sessionStorage.setItem('tekwatt-demo-auth', 'true');
    sessionStorage.setItem('tekwatt-data-mode', 'live');
    setDemo(false); setAuthenticated(true);
  };
  const enterDemo = () => { sessionStorage.setItem('tekwatt-demo-auth', 'true'); sessionStorage.setItem('tekwatt-data-mode', 'demo'); setDemo(true); setAuthenticated(true); };
  const logout = () => { sessionStorage.removeItem('tekwatt-demo-auth'); sessionStorage.removeItem('tekwatt-data-mode'); sessionStorage.removeItem('tekwatt-access-token'); sessionStorage.removeItem('tekwatt-refresh-token'); setAuthenticated(false); };
  return authenticated ? <Portal logout={logout} demo={demo} /> : <Login onLogin={login} onDemo={enterDemo} />;
}
