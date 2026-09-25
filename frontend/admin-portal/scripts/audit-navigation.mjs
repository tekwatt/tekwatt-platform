import assert from 'node:assert/strict';
const navigation = await import('../src/navigation.ts');

const allPermissions = new Set(navigation.pageOrder.filter(page => !navigation.navigationScreens[page]));
for (const screens of Object.values(navigation.navigationScreens)) {
  for (const screen of screens) allPermissions.add(screen);
}

const targets = navigation.pageOrder.flatMap(page => {
  const screens = navigation.navigationScreens[page];
  return screens ? screens.map(view => ({ page, view })) : [{ page }];
});

for (const target of targets) {
  const hash = navigation.navigationHash(target);
  assert.deepEqual(navigation.parseNavigationHash(hash), target, `Route did not round-trip: ${hash}`);
  assert.deepEqual(navigation.resolveNavigationTarget(target, allPermissions), target, `Target was not reachable: ${hash}`);
}

assert.equal(navigation.parseNavigationHash('#/does-not-exist'), undefined);
assert.equal(navigation.parseNavigationHash('#/stations/does-not-exist'), undefined);

const restricted = new Set(['Dashboard', 'Stations']);
assert.deepEqual(
  navigation.resolveNavigationTarget({ page: 'Stations', view: 'Configuration' }, restricted),
  { page: 'Stations', view: 'Stations' },
  'A hidden child screen must fall back to an allowed child in the same section.',
);
assert.deepEqual(
  navigation.resolveNavigationTarget({ page: 'Admins', view: 'Install Modules' }, restricted),
  { page: 'Dashboard' },
  'A hidden section must fall back to the first accessible section.',
);

console.log(`Navigation audit passed: ${targets.length} pages and submenu routes checked.`);
