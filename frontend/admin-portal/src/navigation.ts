export type Page =
  | 'Dashboard'
  | 'Stations'
  | 'Sessions'
  | 'Payments'
  | 'Users'
  | 'Managers'
  | 'Content'
  | 'Reports'
  | 'Support'
  | 'Roles'
  | 'Admins'
  | 'Settings';

export type NavigationTarget = { page: Page; view?: string };

export const navigationScreens: Partial<Record<Page, readonly string[]>> = {
  Stations: ['Stations', 'Chargers', 'Connectors', 'Map View', 'Tariff Management', 'Configuration'],
  Sessions: ['Charging Sessions', 'Live Monitoring', 'Remote Control'],
  Payments: ['Transactions', 'Scan & Pay', 'Wallet', 'Invoices', 'Payment Gateways'],
  Users: ['Customers', 'Partners', 'Technicians', 'RFID Cards'],
  Managers: ['Employees', 'Franchises', 'Vendors', 'Projects', 'CRM / Leads'],
  Content: ['Notifications', 'Offers & Coupons'],
  Reports: ['Reports', 'Analytics', 'Station Performance', 'Daily Report', 'Diagnostics', 'OCPP Logs', 'Firmware'],
  Support: ['Support Tickets', 'Maintenance', 'AMC Contracts'],
  Admins: ['Administrators', 'API Keys', 'Install Modules'],
  Settings: ['General Settings', 'Account Settings'],
};

export const pageOrder: readonly Page[] = [
  'Dashboard', 'Stations', 'Sessions', 'Payments', 'Users', 'Managers',
  'Content', 'Reports', 'Support', 'Roles', 'Admins', 'Settings',
];

const slugify = (value: string) => value
  .trim()
  .toLowerCase()
  .replace(/&/g, ' and ')
  .replace(/[^a-z0-9]+/g, '-')
  .replace(/^-|-$/g, '');

export function isPageAccessible(page: Page, enabledPermissions: ReadonlySet<string>) {
  if (page === 'Dashboard') return enabledPermissions.has('Dashboard');
  const screens = navigationScreens[page];
  return screens ? screens.some(screen => enabledPermissions.has(screen)) : enabledPermissions.has(page);
}

export function allowedNavigationScreens(page: Page, enabledPermissions: ReadonlySet<string>) {
  return (navigationScreens[page] ?? []).filter(screen => enabledPermissions.has(screen));
}

export function firstAccessibleTarget(enabledPermissions: ReadonlySet<string>): NavigationTarget {
  for (const page of pageOrder) {
    if (!isPageAccessible(page, enabledPermissions)) continue;
    const view = allowedNavigationScreens(page, enabledPermissions)[0];
    return view ? { page, view } : { page };
  }
  return { page: 'Dashboard' };
}

export function resolveNavigationTarget(target: NavigationTarget, enabledPermissions: ReadonlySet<string>): NavigationTarget {
  if (!isPageAccessible(target.page, enabledPermissions)) return firstAccessibleTarget(enabledPermissions);
  const screens = allowedNavigationScreens(target.page, enabledPermissions);
  if (!navigationScreens[target.page]) return { page: target.page };
  const view = target.view && screens.includes(target.view) ? target.view : screens[0];
  return view ? { page: target.page, view } : firstAccessibleTarget(enabledPermissions);
}

export function navigationHash(target: NavigationTarget) {
  const parts = [slugify(target.page)];
  if (target.view) parts.push(slugify(target.view));
  return `#/${parts.join('/')}`;
}

export function parseNavigationHash(hash: string): NavigationTarget | undefined {
  const parts = hash.replace(/^#\/?/, '').split('/').filter(Boolean);
  if (!parts.length) return undefined;
  const page = pageOrder.find(item => slugify(item) === parts[0]);
  if (!page) return undefined;
  const screens = navigationScreens[page];
  if (!screens) return parts.length === 1 ? { page } : undefined;
  if (parts.length === 1) return { page, view: screens[0] };
  const view = screens.find(item => slugify(item) === parts[1]);
  return view && parts.length === 2 ? { page, view } : undefined;
}

export function targetsEqual(left: NavigationTarget, right: NavigationTarget) {
  return left.page === right.page && left.view === right.view;
}
