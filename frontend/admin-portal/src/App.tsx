import { FormEvent, useMemo, useState } from 'react';
import {
  Activity, Bell, Bolt, Building2, ChevronDown, CircleDollarSign,
  CreditCard, FileBarChart, Gauge, Headphones, LayoutDashboard, LogOut,
  Menu, Moon, PlugZap, Plus, Search, Settings, ShieldCheck, Sun,
  Users, WalletCards, X, Zap,
} from 'lucide-react';

type Page = 'Dashboard' | 'Stations' | 'Sessions' | 'Payments' | 'Users' | 'Reports' | 'Support' | 'Settings';

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

function Login({ onLogin }: { onLogin: () => void }) {
  const [email, setEmail] = useState('admin@tekwatt.in');
  const [password, setPassword] = useState('admin123');
  const submit = (event: FormEvent) => { event.preventDefault(); if (email && password) onLogin(); };
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
        <label>Password<input type="password" value={password} onChange={e => setPassword(e.target.value)} /></label>
        <button className="primary wide" type="submit">Sign in <Bolt size={18} /></button>
        <small>Demo: admin@tekwatt.in / admin123</small>
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

function Header({ page, dark, toggleDark, openMenu, logout }: { page: Page; dark: boolean; toggleDark: () => void; openMenu: () => void; logout: () => void }) {
  return <header><button className="icon mobile-only" onClick={openMenu}><Menu /></button><div><small>OPERATIONS</small><h2>{page}</h2></div><div className="header-actions"><label className="search"><Search size={17} /><input placeholder="Search network" /></label><span className="live"><i /> LIVE</span><button className="icon" onClick={toggleDark} aria-label="Toggle theme">{dark ? <Sun /> : <Moon />}</button><button className="icon notification"><Bell /><i /></button><button className="profile"><span className="avatar">BA</span><span><strong>Bharanidharan</strong><small>System Admin</small></span><ChevronDown size={16} /></button><button className="icon logout" onClick={logout} title="Sign out"><LogOut /></button></div></header>;
}

function Metric({ label, value, trend, icon: Icon, tone }: { label: string; value: string; trend: string; icon: typeof Gauge; tone: string }) {
  return <article className="metric"><div className={`metric-icon ${tone}`}><Icon /></div><div><small>{label}</small><strong>{value}</strong><span>{trend}</span></div></article>;
}

function Dashboard({ setPage }: { setPage: (p: Page) => void }) {
  return <>
    <section className="welcome"><div><span className="eyebrow">THURSDAY, 14 AUGUST</span><h1>Good afternoon, Bharanidharan.</h1><p>Your charging network is performing well today.</p></div><button className="primary" onClick={() => setPage('Stations')}><Plus size={18} /> Add station</button></section>
    <section className="metrics">
      <Metric label="ONLINE STATIONS" value="5 / 5" trend="100% availability" icon={Building2} tone="green" />
      <Metric label="ACTIVE SESSIONS" value="18" trend="+12% from yesterday" icon={PlugZap} tone="blue" />
      <Metric label="ENERGY TODAY" value="428.6 kWh" trend="+8.4% this week" icon={Bolt} tone="amber" />
      <Metric label="REVENUE TODAY" value="₹12,840" trend="+16.2% this week" icon={CircleDollarSign} tone="violet" />
    </section>
    <section className="dashboard-grid">
      <article className="card performance"><div className="card-head"><div><small>NETWORK PERFORMANCE</small><h3>Energy delivery</h3></div><select defaultValue="7"><option value="7">Last 7 days</option><option value="30">Last 30 days</option></select></div><div className="chart-meta"><strong>2,864.8 <small>kWh</small></strong><span>↑ 11.4%</span></div><div className="bars">{[42,58,48,72,66,86,78].map((h, i) => <div key={i}><span style={{height:`${h}%`}} /><small>{['Fri','Sat','Sun','Mon','Tue','Wed','Thu'][i]}</small></div>)}</div></article>
      <article className="card network"><div className="card-head"><div><small>LIVE NETWORK</small><h3>Station availability</h3></div><button className="link" onClick={() => setPage('Stations')}>View all</button></div><div className="donut"><div><strong>95%</strong><small>Available</small></div></div><div className="legend"><span><i className="online" /> Online <b>19</b></span><span><i className="busy" /> In use <b>7</b></span><span><i className="offline" /> Offline <b>1</b></span></div></article>
    </section>
    <section className="dashboard-grid lower"><StationCard compact setPage={setPage} /><SessionsCard compact setPage={setPage} /></section>
  </>;
}

function StationCard({ compact = false, setPage }: { compact?: boolean; setPage?: (p: Page) => void }) {
  const list = compact ? stations.slice(0, 4) : stations;
  return <article className="card table-card"><div className="card-head"><div><small>INFRASTRUCTURE</small><h3>Station health</h3></div>{compact && <button className="link" onClick={() => setPage?.('Stations')}>View all</button>}</div><div className="station-list">{list.map(s => <div className="station-row" key={s.name}><div className="station-symbol"><Bolt size={18} /></div><div className="station-name"><strong>{s.name}</strong><small>{s.city} · {s.chargers} chargers</small></div><div className="usage"><span style={{width:`${s.usage}%`}} /></div><span className={`status ${s.status.toLowerCase()}`}>{s.status}</span></div>)}</div></article>;
}

function SessionsCard({ compact = false, setPage }: { compact?: boolean; setPage?: (p: Page) => void }) {
  return <article className="card table-card"><div className="card-head"><div><small>LIVE ACTIVITY</small><h3>Recent sessions</h3></div>{compact && <button className="link" onClick={() => setPage?.('Sessions')}>View all</button>}</div><div className="responsive-table"><table><thead><tr><th>Session</th><th>Station</th><th>Energy</th><th>Amount</th><th>Status</th></tr></thead><tbody>{sessions.map(s => <tr key={s.id}><td><strong>{s.id}</strong><small>{s.time}</small></td><td>{s.station}</td><td>{s.energy}</td><td>{s.amount}</td><td><span className={`status ${s.status.toLowerCase()}`}>{s.status}</span></td></tr>)}</tbody></table></div></article>;
}

function GenericPage({ page }: { page: Page }) {
  const content: Record<Exclude<Page, 'Dashboard' | 'Stations' | 'Sessions'>, { icon: typeof Gauge; title: string; copy: string; stats: string[] }> = {
    Payments: { icon: CreditCard, title: 'Payment operations', copy: 'Track collections, settlements, refunds and payment gateway health.', stats: ['₹3.84L collected', '98.7% success rate', '₹18,400 pending'] },
    Users: { icon: Users, title: 'Driver community', copy: 'Manage customers, partners, fleet accounts and access controls.', stats: ['2,846 customers', '128 new this month', '42 fleet accounts'] },
    Reports: { icon: FileBarChart, title: 'Reports & analytics', copy: 'Turn charging, revenue and energy data into operational insight.', stats: ['Daily operations', 'Revenue summary', 'Energy consumption'] },
    Support: { icon: Headphones, title: 'Reviews & support', copy: 'Resolve customer queries and monitor service satisfaction.', stats: ['6 open tickets', '2.4 hr response', '4.8 / 5 rating'] },
    Settings: { icon: Settings, title: 'Platform settings', copy: 'Configure organization, tariffs, notifications and integrations.', stats: ['Organization', 'Tariffs', 'Integrations'] },
  };
  const item = content[page as keyof typeof content];
  return <section><div className="page-title"><span className="eyebrow">TEKWATT NEXUS</span><h1>{item.title}</h1><p>{item.copy}</p></div><div className="feature-grid">{item.stats.map((stat, i) => <article className="card feature" key={stat}><span><item.icon /></span><small>{['OVERVIEW','PERFORMANCE','ACTION'][i]}</small><strong>{stat}</strong><button>Open details →</button></article>)}</div></section>;
}

function Portal({ logout }: { logout: () => void }) {
  const [page, setPage] = useState<Page>('Dashboard');
  const [dark, setDark] = useState(false);
  const [menu, setMenu] = useState(false);
  const body = useMemo(() => page === 'Dashboard' ? <Dashboard setPage={setPage} /> : page === 'Stations' ? <><div className="page-title"><span className="eyebrow">INFRASTRUCTURE</span><h1>Charging stations</h1><p>Monitor availability and utilization across the network.</p></div><StationCard /></> : page === 'Sessions' ? <><div className="page-title"><span className="eyebrow">CHARGING ACTIVITY</span><h1>Charging sessions</h1><p>Follow live and completed vehicle charging activity.</p></div><SessionsCard /></> : <GenericPage page={page} />, [page]);
  return <div className={`app ${dark ? 'dark' : ''}`}><Sidebar page={page} setPage={setPage} open={menu} close={() => setMenu(false)} />{menu && <div className="scrim" onClick={() => setMenu(false)} />}<div className="shell"><Header page={page} dark={dark} toggleDark={() => setDark(!dark)} openMenu={() => setMenu(true)} logout={logout} /><main className="content">{body}</main></div></div>;
}

export default function App() {
  const [authenticated, setAuthenticated] = useState(() => sessionStorage.getItem('tekwatt-demo-auth') === 'true');
  const login = () => { sessionStorage.setItem('tekwatt-demo-auth', 'true'); setAuthenticated(true); };
  const logout = () => { sessionStorage.removeItem('tekwatt-demo-auth'); setAuthenticated(false); };
  return authenticated ? <Portal logout={logout} /> : <Login onLogin={login} />;
}
