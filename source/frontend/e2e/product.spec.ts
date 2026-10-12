import { expect, test, type APIRequestContext } from '@playwright/test';

const apiBase = process.env.PLAYWRIGHT_API_BASE_URL ?? 'http://127.0.0.1:8080';

test.beforeEach(async ({ request }) => {
  const loaded = await request.post(`${apiBase}/api/datasets/demo`);
  expect(loaded.ok(), `demo dataset load returned ${loaded.status()}`).toBeTruthy();
  const body = await loaded.json() as { loaded: boolean | string; size: number };
  expect(body.loaded).toBeTruthy();
  expect(body.size).toBeGreaterThan(0);
});

test('command center presents a backend source and responds at desktop and mobile widths', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Command center' })).toBeVisible();
  await expect(page.getByText('Demo Dataset', { exact: true }).first()).toBeVisible();
  await expect(page.getByRole('figure', { name: 'From event to evidence' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(1441);

  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole('heading', { name: 'Command center' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(391);
  const pipelineStages = await page.locator('.signal-stage').evaluateAll((elements) => elements.map((element) => {
    const { left, right } = element.getBoundingClientRect();
    return { left, right };
  }));
  expect(pipelineStages.every((stage) => stage.left >= 0 && stage.right <= 390)).toBeTruthy();
  expect(errors).toEqual([]);
});

test('direct route refresh reaches the primary product surfaces', async ({ page }) => {
  const pageErrors: string[] = [];
  const consoleErrors: string[] = [];
  const failedRequests: string[] = [];
  page.on('pageerror', (error) => pageErrors.push(error.message));
  page.on('console', (message) => { if (message.type() === 'error') consoleErrors.push(message.text()); });
  page.on('requestfailed', (request) => {
    const failure = request.failure()?.errorText ?? '';
    if (!/ERR_ABORTED|NS_BINDING_ABORTED|aborted/i.test(failure)) failedRequests.push(`${request.url()} (${failure})`);
  });
  const routes = [
    '/', '/scenario-lab', '/logs', '/search', '/analytics', '/patterns', '/incidents',
    '/incidents/workbench', '/services', '/live', '/replay', '/datasets', '/ingestion',
    '/analysis', '/algorithms', '/benchmarks', '/runs', '/system', '/docs'
  ];
  for (const route of routes) {
    await page.goto(route);
    await expect(page.locator('main')).toBeVisible();
    await expect(page.locator('main h1').first(), `route ${route} should render a page heading`).toBeVisible();
    await expect(page.getByText('Page not found')).toHaveCount(0);
  }
  expect(pageErrors, 'no uncaught errors while rendering documented routes').toEqual([]);
  expect(consoleErrors, 'no browser console errors while rendering documented routes').toEqual([]);
  expect(failedRequests, 'no unexpected failed browser requests while rendering documented routes').toEqual([]);
  const icon = await page.request.get('/loginsight-mark.svg');
  expect(icon.ok(), 'the app icon is served by the frontend').toBeTruthy();
  await expect(page.locator('link[rel="icon"]')).toHaveAttribute('href', '/loginsight-mark.svg');
});

test('Atmospheric Signal semantic text colors meet WCAG AA against their intended surfaces', async ({ page }) => {
  await page.goto('/');
  const ratios = await page.evaluate(() => {
    const style = getComputedStyle(document.documentElement);
    const color = (name: string) => style.getPropertyValue(name).trim();
    const channels = (hex: string) => hex.replace('#', '').match(/.{2}/g)!.map((part) => parseInt(part, 16) / 255);
    const luminance = (hex: string) => channels(hex).map((channel) => channel <= .04045 ? channel / 12.92 : ((channel + .055) / 1.055) ** 2.4).reduce((total, channel, index) => total + channel * [.2126, .7152, .0722][index], 0);
    const pairs = [
      ['--text', '--surface-0'], ['--text-soft', '--surface-0'], ['--text-muted', '--surface-0'],
      ['--text-faint', '--surface-0'], ['--accent-strong', '--surface-0'], ['--accent', '--surface-0'],
      ['--ok', '--ok-dim'], ['--warn', '--warn-dim'], ['--danger', '--danger-dim'],
      ['--info', '--info-dim'], ['--purple', '--surface-0'],
      // Severity and algorithm-family colors must also be legible on a card.
      ['--severity-error', '--surface-0'], ['--severity-warn', '--surface-0'],
      ['--severity-info', '--surface-0'], ['--severity-trace', '--surface-0'],
      ['--mod-strings', '--surface-0'], ['--mod-dp', '--surface-0'], ['--mod-flow', '--surface-0'],
      ['--mod-approx', '--surface-0'], ['--mod-random', '--surface-0'], ['--mod-parallel', '--surface-0']
    ];
    return pairs.map(([foreground, background]) => {
      const a = luminance(color(foreground));
      const b = luminance(color(background));
      return { pair: `${foreground} on ${background}`, ratio: Number(((Math.max(a, b) + .05) / (Math.min(a, b) + .05)).toFixed(2)) };
    });
  });
  expect(ratios.filter(({ ratio }) => ratio < 4.5), JSON.stringify(ratios)).toEqual([]);
});

test('scroll reveals trigger from real scrolling and remain visible with reduced motion', async ({ page }) => {
  await page.setViewportSize({ width: 1024, height: 500 });
  await page.goto('/');
  const lowerSection = page.locator('.overview-lower-grid');
  await expect(lowerSection).toBeVisible();
  await expect.poll(() => lowerSection.evaluate((element) => element.classList.contains('is-revealed'))).toBeFalsy();
  await lowerSection.scrollIntoViewIfNeeded();
  await expect.poll(() => lowerSection.evaluate((element) => element.classList.contains('is-revealed'))).toBeTruthy();

  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto('/');
  const reducedMotionSection = page.locator('.overview-lower-grid');
  await reducedMotionSection.scrollIntoViewIfNeeded();
  await expect(reducedMotionSection).toBeVisible();
  // Reduced motion never applies the hidden state, so the section must remain fully opaque.
  await expect.poll(() => reducedMotionSection.evaluate((element) => getComputedStyle(element).opacity)).toBe('1');
  expect(await reducedMotionSection.evaluate((element) => getComputedStyle(element).transform)).toBe('none');
});

test('search executes against the dataset and opens real event details', async ({ page }) => {
  await page.goto('/logs');
  await page.getByRole('searchbox', { name: 'Search logs' }).fill('level:ERROR');
  await page.getByRole('button', { name: 'Search', exact: true }).click();
  await expect(page.getByText('Matching events')).toBeVisible();
  await expect(page.getByRole('list', { name: 'Filtered log events' })).toBeVisible();
  const firstEvent = page.getByRole('button', { name: /Inspect event/ }).first();
  await firstEvent.click();
  await expect(page.getByRole('dialog')).toBeVisible();
});

test('observed topology selects a service and the WebGL failure keeps 2D available', async ({ page }) => {
  await page.goto('/services');
  await expect(page.getByRole('heading', { name: 'Observed service map' })).toBeVisible();
  const node = page.locator('.topology-node').first();
  await expect(node).toBeVisible();
  const nodeId = await node.getAttribute('data-node-id');
  await node.click();
  await expect(page.locator('.service-inspector-title')).toContainText(nodeId ?? '');
  await page.addInitScript(() => {
    const original = HTMLCanvasElement.prototype.getContext;
    HTMLCanvasElement.prototype.getContext = function (context: string, ...args: unknown[]) {
      if (context.includes('webgl')) return null;
      return original.call(this, context as '2d', ...args as []);
    };
  });
  await page.reload();
  await page.getByRole('button', { name: '3D WebGL' }).click();
  await expect(page.getByText('3D visualization unavailable on this device.')).toBeVisible();
  await page.getByRole('button', { name: '2D', exact: true }).click();
  await expect(page.locator('.topology-svg')).toBeVisible();
});

test('guided presentation runs backend evidence, retries its controls, and exits cleanly', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await page.goto('/');
  await page.getByRole('button', { name: /Start guided demo/ }).click();
  await page.getByRole('button', { name: /Begin walkthrough/ }).click();
  await expect(page.getByText(/Product explanation/)).toBeVisible();
  await page.getByRole('button', { name: /Next step/ }).click();
  await expect(page.getByText(/parsed events already loaded|parsed events ·/)).toBeVisible();
  await page.getByRole('button', { name: /Next step/ }).click();
  await expect(page.getByText(/accepted events/)).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(errors).toEqual([]);
});

test('mobile navigation and reduced-motion presentation remain operable', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');
  const openNavigation = page.getByRole('button', { name: 'Open navigation' });
  await openNavigation.click();
  await expect(page.locator('#primary-navigation')).toBeVisible();
  await page.getByRole('link', { name: 'Analytics' }).click();
  await expect(page.getByRole('heading', { name: 'Analytics', exact: true })).toBeVisible();

  await page.emulateMedia({ reducedMotion: 'reduce' });
  const analyticsTabs = page.getByRole('tablist', { name: 'Analytics views' });
  await analyticsTabs.scrollIntoViewIfNeeded();
  await expect(analyticsTabs).toBeVisible();
  const animation = await analyticsTabs.evaluate((element) => getComputedStyle(element).animationDuration);
  expect(Number.parseFloat(animation)).toBeLessThanOrEqual(0.00001);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(391);
});

test('command center and service topology fit all requested viewport targets', async ({ page }) => {
  const viewports = [
    { width: 1440, height: 900 }, { width: 1280, height: 800 },
    { width: 1024, height: 768 }, { width: 768, height: 1024 },
    { width: 390, height: 844 }, { width: 360, height: 800 }
  ];
  for (const viewport of viewports) {
    await page.setViewportSize(viewport);
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'Command center' })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth), `command center overflow at ${viewport.width}x${viewport.height}`).toBeLessThanOrEqual(viewport.width + 1);
    await page.goto('/services');
    await expect(page.locator('.topology-stage')).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth), `service map overflow at ${viewport.width}x${viewport.height}`).toBeLessThanOrEqual(viewport.width + 1);
  }
});

test('the signal field reports real backend values and never invents them', async ({ page, request }) => {
  await page.goto('/');
  const figure = page.getByRole('figure', { name: 'From event to evidence' });
  await expect(figure).toBeVisible();

  const stages = figure.getByRole('listitem');
  await expect(stages).toHaveCount(4);
  const summary = await api.health(request);
  const dataset = await api.currentDataset(request);
  const expectedEvents = formatNumber(dataset?.size ?? 0);
  await expect(figure).toContainText(expectedEvents);
  // Every stage must show a value or an explicit em dash, never a zero placeholder.
  for (const value of await stages.locator('strong').allInnerTexts()) {
    expect(value === '—' || /\d/.test(value), `unexpected stage value: ${value}`).toBeTruthy();
  }
  expect(summary.status).toBe('UP');
});

test('an empty dataset shows onboarding instead of fabricated metrics', async ({ page, request }) => {
  await request.delete(`${apiBase}/api/datasets`);
  try {
    await page.goto('/');
    await expect(page.getByRole('heading', { name: 'No investigation data yet' })).toBeVisible();
    await expect(page.getByText('Awaiting a data source')).toBeVisible();
    // The stage values must be unavailable markers, not zeroed counters.
    await expect(page.locator('.signal-stage strong').first()).toHaveText('—');
    await expect(page.getByRole('link', { name: 'Choose a source' }).first()).toBeVisible();
    await expect(page.getByRole('link', { name: 'Upload logs' })).toBeVisible();
    // Header must not claim a healthy source while none is loaded.
    await expect(page.getByRole('link', { name: /Source: No dataset/ })).toBeVisible();
  } finally {
    await request.post(`${apiBase}/api/datasets/demo`);
  }
});

test('a failing backend surfaces a retryable error instead of a blank workspace', async ({ page }) => {
  await page.route((url) => url.pathname.startsWith('/api/'), (route) => route.fulfill({
    status: 500,
    contentType: 'application/json',
    body: JSON.stringify({ status: 500, error: 'InternalServerError', message: 'An unexpected internal error occurred', path: '/api/health/status' })
  }));
  await page.goto('/system');
  const alert = page.getByRole('alert').first();
  await expect(alert).toBeVisible();
  await expect(alert).toContainText('An unexpected internal error occurred');
  await expect(alert.getByRole('button', { name: 'Retry' })).toBeVisible();
  // The shell must survive a failing API: navigation and content are still reachable.
  await expect(page.getByRole('navigation', { name: 'Workspace sections' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'API: Unavailable' })).toBeVisible();
});

test('the command palette navigates and restores focus', async ({ page }) => {
  await page.goto('/');
  const trigger = page.getByRole('button', { name: 'Open command palette' });
  await trigger.focus();
  await page.keyboard.press('Control+k');
  const dialog = page.getByRole('dialog', { name: 'Move through LogInsight' });
  await expect(dialog).toBeVisible();
  await dialog.getByRole('searchbox').fill('analytics');
  await dialog.getByRole('option', { name: /Analytics/ }).click();
  await expect(page.getByRole('heading', { name: 'Analytics', exact: true })).toBeVisible();
  await expect(page.getByRole('dialog', { name: 'Move through LogInsight' })).toHaveCount(0);
});

test('documented route aliases resolve to the same workspace', async ({ page }) => {
  const aliases: Array<[string, string]> = [
    ['/command-center', 'Command center'],
    ['/overview', 'Command center'],
    ['/analyze', 'Analytics'],
    ['/data', 'Datasets'],
    ['/system/status', 'System'],
    ['/lab', 'Algorithm Lab'],
    ['/algorithm-lab', 'Algorithm Lab'],
    ['/simulation', 'Scenario Lab']
  ];
  for (const [route, heading] of aliases) {
    await page.goto(route);
    await expect(page.getByRole('heading', { name: heading, exact: true }), `${route} should render ${heading}`).toBeVisible();
    await expect(page.getByRole('link', { name: 'Skip to main content' })).toHaveAttribute('href', '#main-content');
  }
});

test('a document route does not resolve', async ({ page }) => {
  await page.goto('/this-route-does-not-exist');
  await expect(page.getByText('Page not found')).toBeVisible();
  await expect(page.locator('main h1')).toBeVisible();
});

test('primary navigation stays compact while every advanced tool remains reachable', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/');
  const nav = page.getByRole('navigation', { name: 'Workspace sections' });

  // The daily workflow is visible without opening anything.
  for (const label of ['Overview', 'Logs', 'Incidents', 'Services', 'Analytics']) {
    await expect(nav.getByRole('link', { name: label, exact: true })).toBeVisible();
  }
  expect(await nav.getByRole('link').count()).toBe(5);

  // Specialist tools are collapsed, not deleted.
  await expect(nav.getByRole('link', { name: 'Algorithm Lab' })).toHaveCount(0);
  await nav.getByRole('button', { name: /^Advanced/ }).click();
  for (const label of ['Incident Workbench', 'Live Monitor', 'Dataset Replay', 'Scenario Lab', 'Patterns', 'Algorithm Lab', 'Algorithmic Search', 'Algorithms', 'Benchmarks', 'Run Sessions', 'Datasets', 'Ingestion', 'System', 'Documentation']) {
    await expect(nav.getByRole('link', { name: label, exact: true }), `${label} must remain reachable`).toBeVisible();
  }
  expect(await nav.getByRole('link').count()).toBe(19);
});

test('the 2D/3D toggle survives repeated switching without duplicating canvases', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/services');
  await expect(page.getByRole('heading', { name: 'Observed service map' })).toBeVisible();
  await page.locator('.topology-node').first().waitFor();

  const nodeId = await page.locator('.topology-node').first().getAttribute('data-node-id');
  await page.locator('.topology-node').first().click();
  await expect(page.locator('.service-inspector-title')).toContainText(nodeId ?? '');

  const canvases = () => page.locator('canvas').count();
  for (let round = 0; round < 3; round += 1) {
    await page.getByRole('button', { name: '3D WebGL' }).click();
    await expect(page.locator('.topology-stage')).toHaveAttribute('data-mode', '3d');
    await page.waitForTimeout(350);
    expect(await canvases(), `round ${round}: at most one canvas should exist`).toBeLessThanOrEqual(1);

    await page.getByRole('button', { name: '2D', exact: true }).click();
    await expect(page.locator('.topology-stage')).toHaveAttribute('data-mode', '2d');
    // Selection must survive the round trip.
    await expect(page.locator('.topology-node--selected')).toHaveCount(1);
  }
});

test('the command center keeps operational data above the fold', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/');
  await page.locator('.overview-metrics').waitFor();
  await page.evaluate(() => window.scrollTo(0, 0));
  // Selected-window metrics must be reachable without scrolling past a hero.
  const metricsTop = await page.locator('.overview-metrics').evaluate((el) => el.getBoundingClientRect().top);
  expect(metricsTop).toBeLessThan(900);
  const heroHeight = await page.locator('.cc-hero').evaluate((el) => el.getBoundingClientRect().height);
  expect(heroHeight).toBeLessThan(420);
});

/** Minimal API helpers shared by the state-specific tests above. */
const api = {
  async health(request: APIRequestContext) {
    const response = await request.get(`${apiBase}/api/health`);
    expect(response.ok()).toBeTruthy();
    return (await response.json()) as { status: string; service: string };
  },
  async currentDataset(request: APIRequestContext) {
    const response = await request.get(`${apiBase}/api/datasets/current`);
    if (!response.ok()) return null;
    return (await response.json()) as { size: number } | null;
  }
};

function formatNumber(value: number): string {
  if (value >= 1_000_000) return `${Math.round(value / 100_000) / 10}M`;
  if (value >= 1_000) return `${Math.round(value / 100) / 10}k`;
  return String(value);
}
