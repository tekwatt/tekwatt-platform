import { FormEvent, useEffect, useMemo, useState } from 'react';
import {
  Activity, Bell, Bolt, Building2, ChevronDown, CircleDollarSign,
  CreditCard, FileBarChart, Gauge, Headphones, LayoutDashboard, LogOut,
  Menu, Moon, PlugZap, Plus, Search, Settings, ShieldCheck, Sun,
  Users, WalletCards, X, Zap,
} from 'lucide-react';
import { api, Charger, ChargingSession, Payment, Report, Tenant, UserProfile } from './api';

type Page = 'Dashboard' | 'Stations' | 'Sessions' | 'Payments' | 'Users' | 'Reports' | 'Support' | 'Settings';
type LiveData = { tenant?: Tenant; chargers: Charger[]; sessions: ChargingSession[]; payments: Payment[]; users: UserProfile[]; reports: Report[] };

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

function Sidebar({ page, setPage, open, close }: { page: Page; setPage: (page: Page) => void; open: boolean; close: () => void }) {
  return <aside className={`sidebar ${open ? 'open' : ''}`}>
    <div className="sidebar-head"><Brand /><button className="icon mobile-only" onClick={close}><X /></button></div>
    <div className="workspace"><span className="avatar mini">TW</span><div><small>WORKSPACE</small><strong>TekWatt India</strong></div><ChevronDown size={16} /></div>
    <nav>{nav.map(item => <button key={item.label} className={page === item.label ? 'active' : ''} onClick={() => { setPage(item.label); close(); }}><item.icon size={19} /><span>{item.label}</span>{item.badge && <em>{item.badge}</em>}</button>)}</nav>
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

function Dashboard({ setPage, data, demo }: { setPage: (p: Page) => void; data: LiveData; demo: boolean }) {
  const stationRows = demo ? stations : data.chargers.map((charger, index) => ({ name: charger.stationId, city: `${charger.vendor} ${charger.model}`, chargers: 1, power: charger.protocolVersion, status: charger.status === 'AVAILABLE' || charger.status === 'ONLINE' ? 'Online' : charger.status, usage: 35 + (index * 13) % 55 }));
  const sessionRows = demo ? sessions : data.sessions.map(session => ({ id: session.transactionId || session.id, station: data.chargers.find(charger => charger.id === session.chargerId)?.stationId ?? 'Unknown charger', energy: `${session.energyKwh ?? 0} kWh`, amount: `${session.currency ?? 'INR'} ${session.totalCost ?? 0}`, status: session.status, time: session.startedAt ? new Date(session.startedAt).toLocaleString() : '—' }));
  const online = demo ? 5 : data.chargers.filter(charger => ['AVAILABLE','ONLINE','CHARGING'].includes(charger.status)).length;
  const active = demo ? 18 : data.sessions.filter(session => ['ACTIVE','CHARGING','STARTED'].includes(session.status)).length;
  const energy = demo ? 428.6 : data.sessions.reduce((sum, session) => sum + Number(session.energyKwh ?? 0), 0);
  const revenue = demo ? 12840 : data.payments.filter(payment => ['COMPLETED','SUCCESS','PAID'].includes(payment.status ?? '')).reduce((sum, payment) => sum + Number(payment.amount ?? 0), 0);
  return <>
    <section className="welcome"><div><span className="eyebrow">THURSDAY, 14 AUGUST</span><h1>Good afternoon, Bharanidharan.</h1><p>Your charging network is performing well today.</p></div><button className="primary" onClick={() => setPage('Stations')}><Plus size={18} /> Add station</button></section>
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

function GenericPage({ page, data }: { page: Page; data: LiveData }) {
  const content: Record<Exclude<Page, 'Dashboard' | 'Stations' | 'Sessions'>, { icon: typeof Gauge; title: string; copy: string; stats: string[] }> = {
    Payments: { icon: CreditCard, title: 'Payment operations', copy: 'Track collections, settlements, refunds and payment gateway health.', stats: [`${data.payments.length} payment records`, `${data.payments.filter(p => ['COMPLETED','SUCCESS','PAID'].includes(p.status ?? '')).length} completed`, `${data.payments.filter(p => ['PENDING','CREATED'].includes(p.status ?? '')).length} pending`] },
    Users: { icon: Users, title: 'Driver community', copy: 'Manage customers, partners, fleet accounts and access controls.', stats: [`${data.users.length} customer profiles`, `${data.users.filter(u => u.status === 'ACTIVE').length} active`, data.tenant?.name ?? 'Current tenant'] },
    Reports: { icon: FileBarChart, title: 'Reports & analytics', copy: 'Turn charging, revenue and energy data into operational insight.', stats: [`${data.reports.length} generated reports`, `${data.reports.filter(r => r.status === 'COMPLETED').length} ready`, `${data.reports.filter(r => r.status === 'PENDING').length} pending`] },
    Support: { icon: Headphones, title: 'Reviews & support', copy: 'Resolve customer queries and monitor service satisfaction.', stats: ['6 open tickets', '2.4 hr response', '4.8 / 5 rating'] },
    Settings: { icon: Settings, title: 'Platform settings', copy: 'Configure organization, tariffs, notifications and integrations.', stats: ['Organization', 'Tariffs', 'Integrations'] },
  };
  const item = content[page as keyof typeof content];
  return <section><div className="page-title"><span className="eyebrow">TEKWATT NEXUS</span><h1>{item.title}</h1><p>{item.copy}</p></div><div className="feature-grid">{item.stats.map((stat, i) => <article className="card feature" key={stat}><span><item.icon /></span><small>{['OVERVIEW','PERFORMANCE','STATUS'][i]}</small><strong>{stat}</strong><span className="connected-label">Connected to API Gateway</span></article>)}</div></section>;
}

function Portal({ logout, demo }: { logout: () => void; demo: boolean }) {
  const [page, setPage] = useState<Page>('Dashboard');
  const [dark, setDark] = useState(false);
  const [menu, setMenu] = useState(false);
  const [data, setData] = useState<LiveData>({ chargers: [], sessions: [], payments: [], users: [], reports: [] });
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
        api.chargers(tenant.id), api.sessions(tenant.id), api.payments(tenant.id), api.users(tenant.id), api.reports(tenant.id),
      ]);
      const failed = results.filter(result => result.status === 'rejected').length;
      const value = <T,>(index: number) => results[index].status === 'fulfilled' ? results[index].value as T[] : [];
      setData({ tenant, chargers: value<Charger>(0), sessions: value<ChargingSession>(1), payments: value<Payment>(2), users: value<UserProfile>(3), reports: value<Report>(4) });
      if (failed) setLoadError(`${failed} service${failed > 1 ? 's are' : ' is'} temporarily unavailable. Available data is still shown.`);
    } catch (reason) { setLoadError(reason instanceof Error ? reason.message : 'Backend data could not be loaded'); }
    finally { setLoading(false); }
  };
  useEffect(() => { void refresh(); }, [demo]);
  const stationRows = demo ? stations : data.chargers.map((charger, index) => ({ name: charger.stationId, city: `${charger.vendor} ${charger.model}`, chargers: 1, power: charger.protocolVersion, status: charger.status === 'AVAILABLE' || charger.status === 'ONLINE' ? 'Online' : charger.status, usage: 35 + (index * 13) % 55 }));
  const sessionRows = demo ? sessions : data.sessions.map(session => ({ id: session.transactionId || session.id, station: data.chargers.find(charger => charger.id === session.chargerId)?.stationId ?? 'Unknown charger', energy: `${session.energyKwh ?? 0} kWh`, amount: `${session.currency ?? 'INR'} ${session.totalCost ?? 0}`, status: session.status, time: session.startedAt ? new Date(session.startedAt).toLocaleString() : '—' }));
  const body = useMemo(() => page === 'Dashboard' ? <Dashboard setPage={setPage} data={data} demo={demo} /> : page === 'Stations' ? <><div className="page-title"><span className="eyebrow">INFRASTRUCTURE</span><h1>Charging stations</h1><p>Monitor availability and utilization across the network.</p></div><StationCard items={stationRows} /></> : page === 'Sessions' ? <><div className="page-title"><span className="eyebrow">CHARGING ACTIVITY</span><h1>Charging sessions</h1><p>Follow live and completed vehicle charging activity.</p></div><SessionsCard items={sessionRows} /></> : <GenericPage page={page} data={data} />, [page, data, demo]);
  return <div className={`app ${dark ? 'dark' : ''}`}><Sidebar page={page} setPage={setPage} open={menu} close={() => setMenu(false)} />{menu && <div className="scrim" onClick={() => setMenu(false)} />}<div className="shell"><Header page={page} dark={dark} toggleDark={() => setDark(!dark)} openMenu={() => setMenu(true)} logout={logout} navigate={setPage} /><main className="content">{demo && <div className="mode-banner">Demo data mode <button onClick={logout}>Connect backend</button></div>}{loading && <div className="loading-bar">Loading data from API Gateway…</div>}{loadError && <div className="api-error"><span>{loadError}</span><button onClick={refresh}>Retry</button></div>}{!loading && body}</main></div></div>;
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
