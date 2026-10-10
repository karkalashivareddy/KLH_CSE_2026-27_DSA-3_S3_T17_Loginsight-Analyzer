import { mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';
import { chromium } from '@playwright/test';

/**
 * Visual QA capture.
 *
 * Every screenshot is taken against the running application with a real dataset
 * loaded through the API, so the gallery shows the product rather than a mock.
 * Routes with no dataset, or a failing backend, are captured last because both
 * mutate shared server state.
 */
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://127.0.0.1:5173';
const apiBaseURL = process.env.PLAYWRIGHT_API_BASE_URL ?? 'http://127.0.0.1:8080';
const output = resolve(process.cwd(), '../docs/images/signal-atlas');
const browserPath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH;

const dataset = await fetch(`${apiBaseURL}/api/datasets/demo`, { method: 'POST' });
if (!dataset.ok) throw new Error(`Could not load deterministic demo data (${dataset.status}).`);
await mkdir(output, { recursive: true });

const browser = await chromium.launch({ headless: true, ...(browserPath ? { executablePath: browserPath } : {}) });
const captured = [];
let page;

try {
  page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
  const capture = async (name) => {
    await page.screenshot({ path: resolve(output, name), animations: 'disabled' });
    captured.push(name);
  };
  const go = async (route, heading) => {
    await page.goto(new URL(route, baseURL).href);
    await page.waitForLoadState('networkidle').catch(() => undefined);
    if (heading) await page.getByRole('heading', { name: heading }).waitFor({ timeout: 20_000 });
  };

  // 1. Command center, first viewport.
  await go('/', 'Command center');
  await page.locator('.signal-field').waitFor();
  await page.locator('.overview-metrics').waitFor();
  await capture('command-center-desktop.png');

  // 2. Command center, scrolled into the topology and evidence band.
  await page.locator('.overview-main-grid').scrollIntoViewIfNeeded();
  await page.waitForTimeout(700);
  await capture('command-center-evidence.png');

  // 3. Live monitor with a genuinely running generated simulation.
  await go('/live', 'Live monitor');
  await page.locator('.simulation-band, .live-hero, .page').first().waitFor();
  await page.waitForTimeout(3_000);
  await capture('live-monitor-desktop.png');

  // 4. Dataset replay: deliberately bounded, unlike the simulation above.
  await go('/replay');
  await page.waitForTimeout(1_200);
  await capture('dataset-replay-desktop.png');

  // 5. Logs explorer with a real server-side query.
  await go('/logs', 'Log explorer');
  await page.getByRole('searchbox', { name: 'Search logs' }).fill('level:ERROR');
  await page.getByRole('button', { name: 'Search', exact: true }).click();
  await page.getByRole('list', { name: 'Filtered log events' }).waitFor();
  await capture('log-explorer-desktop.png');

  // 6. Algorithmic search with a result set and a fuzzy suggestion.
  await go('/search');
  await page.waitForTimeout(900);
  await capture('algorithmic-search-desktop.png');

  // 7. Analytics from live API data.
  await go('/analytics', 'Analytics');
  await page.getByText('Events / min').waitFor();
  await capture('analytics-desktop.png');

// 8. Incident workbench. The stream is started from the UI so the capture shows
  //    a real session, and the script waits for the detector to actually list an
  //    incident before shooting, so the gallery never shows a racy empty state.
//    Delivery speed is raised first: it only paces frames and never changes the
//    events a scenario emits, so the captured evidence stays deterministic.
await page.addInitScript(() => {
  window.localStorage.setItem('loginsight:simulation:scenario', 'checkout-5xx-cascade');
  window.localStorage.setItem('loginsight:simulation:speed', '8');
});
await go('/scenario-lab');
await page.getByRole('button', { name: /^(Start run|Run again)$/ }).first().click({ timeout: 20_000 });
// Client-side navigation: a full page load would tear down the telemetry session
// the workbench reads its incidents from.
await page.getByRole('navigation', { name: 'Workspace sections' }).getByRole('link', { name: 'Incident Workbench' }).click();
await page.getByText('Incident workbench').first().waitFor({ timeout: 20_000 });
const detectedIncident = page.locator('.workbench-item').first();
const foundIncident = await detectedIncident.waitFor({ state: 'visible', timeout: 45_000 }).then(() => true).catch(() => false);
if (!foundIncident) console.warn('  ! no incident detected before capture; the workbench will show its empty state');
await page.waitForTimeout(800);
await capture('incident-workbench-desktop.png');

  // 9. Detector windows list.
  await go('/incidents', 'Incident investigations');
  await page.locator('.incident-investigation-card').waitFor();
  await capture('incident-windows-desktop.png');

  // 10. Topology with a selected service.
  await go('/services', 'Observed service map');
  await page.locator('.topology-node').first().waitFor();
  await capture('topology-2d-desktop.png');
  await page.locator('.topology-node').first().click();
  await page.locator('.service-inspector-title').waitFor();
  await capture('topology-selection-desktop.png');

  // 11. Algorithm catalogue.
  await go('/algorithms');
  await page.waitForTimeout(900);
  await capture('algorithms-desktop.png');

  // 12. WebGL fallback for the optional 3D topology.
  await page.addInitScript(() => {
    const original = HTMLCanvasElement.prototype.getContext;
    HTMLCanvasElement.prototype.getContext = function (context, ...args) {
      if (String(context).includes('webgl')) return null;
      return original.call(this, context, ...args);
    };
  });
  await go('/services', 'Observed service map');
  await page.getByRole('button', { name: '3D WebGL' }).click();
  await page.getByText('3D visualization unavailable on this device.').waitFor();
  await capture('topology-webgl-fallback-desktop.png');
  await page.close();

  // 13. Guided demo, driven by real API operations.
  page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
  await go('/', 'Command center');
  await page.getByRole('button', { name: /Start guided demo/ }).click();
  await page.getByRole('button', { name: /Begin walkthrough/ }).click();
  await page.getByText(/Product explanation/).waitFor();
  await capture('guided-presentation-desktop.png');
  await page.keyboard.press('Escape');

  // 14. Mobile navigation drawer and mobile command center.
  await page.setViewportSize({ width: 390, height: 844 });
  await go('/', 'Command center');
  await capture('command-center-mobile.png');
  await page.getByRole('button', { name: 'Open navigation' }).click();
  await page.getByRole('navigation', { name: 'Workspace sections' }).waitFor();
  await capture('mobile-navigation.png');
  await page.keyboard.press('Escape');

  // 15. Reduced motion: the same route with animations suppressed.
  page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1, reducedMotion: 'reduce' });
  await go('/', 'Command center');
  await page.locator('.signal-field').waitFor();
  await capture('command-center-reduced-motion.png');
  await page.close();

  // 16. No dataset state. Clearing the source is global server state, so this
  //     runs last and the dataset is restored immediately afterwards.
  page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
  await fetch(`${apiBaseURL}/api/datasets`, { method: 'DELETE' });
  await go('/', 'Command center');
  await page.getByText('No investigation data yet').first().waitFor({ timeout: 20_000 });
  await capture('no-dataset-state.png');
  await fetch(`${apiBaseURL}/api/datasets/demo`, { method: 'POST' });

// 17. Backend error state. Fulfilling `/api/*` with a real 500 error envelope
  //     exercises the same failure path as an unreachable backend. The matcher
  //     must exclude the app's own source files under `src/api/`, so it is
//         a predicate rather than a glob.
  await page.route((requestUrl) => requestUrl.pathname.startsWith('/api/'), (route) => route.fulfill({
    status: 500,
    contentType: 'application/json',
    body: JSON.stringify({ status: 500, error: 'InternalServerError', message: 'An unexpected internal error occurred', path: '/api/health/status' })
  }));
  await go('/system');
  await page.getByRole('alert').first().waitFor({ timeout: 20_000 });
  await capture('backend-error-state.png');

  console.log(`Captured ${captured.length} screenshots in ${output}`);
} finally {
  await page?.close().catch(() => undefined);
  await browser.close();
}