import { mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';
import { chromium } from '@playwright/test';

/**
 * Report figure capture.
 *
 * Captures exactly the eleven figures used by the final project report, at the
 * framings the report needs, so every figure in the DOCX/PDF can be regenerated
 * from a live run of the application rather than hand-assembled.
 *
 * `capture:visuals` produces the broader QA gallery. This script is the
 * report-specific subset and must stay in sync with the report figure order.
 *
 * Requires the backend and frontend dev servers to already be running, e.g.
 *   backend :18180, frontend :5174
 */
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://127.0.0.1:5174';
const apiBaseURL = process.env.PLAYWRIGHT_API_BASE_URL ?? 'http://127.0.0.1:18180';
const output = resolve(process.cwd(), '../docs/images/report');

const dataset = await fetch(`${apiBaseURL}/api/datasets/demo`, { method: 'POST' });
if (!dataset.ok) throw new Error(`Could not load deterministic demo data (${dataset.status}).`);
await mkdir(output, { recursive: true });

// SwiftShader gives headless Chromium a real WebGL context, so the optional 3D
// topology can be captured as an actual rendered scene instead of its fallback.
const browser = await chromium.launch({
  headless: true,
  args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'],
  ...(process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH } : {})
});

const captured = [];
let page;

try {
  page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
  const capture = async (name) => {
    await page.screenshot({ path: resolve(output, name), animations: 'disabled' });
    captured.push(name);
    console.log(`  + ${name}`);
  };
  const go = async (route, heading) => {
    await page.goto(new URL(route, baseURL).href);
    await page.waitForLoadState('networkidle').catch(() => undefined);
    if (heading) await page.getByRole('heading', { name: heading }).first().waitFor({ timeout: 20_000 });
  };

  // Deterministic run selection: a fixed scenario id and seed keep the captured
  // stream identical on every regeneration.
  await page.addInitScript(() => {
    window.localStorage.setItem('loginsight:simulation:scenario', 'checkout-5xx-cascade');
    window.localStorage.setItem('loginsight:simulation:speed', '8');
  });

  // Figure 1 - Overview: command center, first viewport.
  await go('/', 'Command center');
  await page.locator('.signal-field').waitFor();
  await page.locator('.overview-metrics').waitFor();
  await capture('report-01-command-center.png');

  // Figure 2 - Scenario Lab: the curated scenario catalogue.
  await go('/scenario-lab');
  await page.locator('.scenario-card').first().waitFor({ timeout: 20_000 });
  await page.locator('.scenario-grid').scrollIntoViewIfNeeded();
  await page.waitForTimeout(600);
  await capture('report-02-scenario-catalogue.png');

  // Figure 3 - Scenario Lab: a run actually in progress.
  await go('/scenario-lab');
  await page.getByRole('button', { name: /^(Start run|Run again)$/ }).first().click({ timeout: 20_000 });
  // Wait for the stream to advance so the capture shows a running simulation
  // rather than the pre-run state.
  await page
    .locator('.status-strip-item', { hasText: 'Tick' })
    .waitFor({ timeout: 20_000 })
    .catch(() => undefined);
  await page.waitForTimeout(6_000);
  await capture('report-03-scenario-running.png');

  // Figure 4 - Incident workbench with a detected incident.
  // Client-side navigation only: a full page load would tear down the telemetry
  // session the workbench reads its incidents from.
  await page.getByRole('navigation', { name: 'Workspace sections' }).getByRole('link', { name: 'Incident Workbench' }).click();
  await page.getByText('Incident workbench').first().waitFor({ timeout: 20_000 });
  const detected = page.locator('.workbench-item').first();
  const foundIncident = await detected.waitFor({ state: 'visible', timeout: 45_000 }).then(() => true).catch(() => false);
  if (!foundIncident) console.warn('  ! no incident detected before capture; workbench will show its empty state');
  await page.waitForTimeout(800);
  await capture('report-04-incident-workbench.png');

  // Figure 5 - Live monitor running the generated simulation.
  await go('/live', 'Live monitor');
  await page.locator('.live-metric-strip, .simulation-band, .live-hero').first().waitFor();
  await page.waitForTimeout(3_000);
  await capture('report-05-live-monitor.png');

  // Figure 6 - Topology, accessible 2D graph.
  await go('/services');
  await page.locator('.topology-node').first().waitFor();
  await page.locator('.service-map-card').scrollIntoViewIfNeeded();
  await page.waitForTimeout(800);
  await capture('report-06-topology-2d.png');

  // Figure 7 - Topology, real 3D WebGL scene.
  await page.getByRole('button', { name: '3D WebGL' }).click();
  const fellBack = await page
    .getByText('3D visualization unavailable on this device.')
    .waitFor({ timeout: 5_000 })
    .then(() => true)
    .catch(() => false);
  if (fellBack) throw new Error('3D WebGL fell back instead of rendering; the report figure would be wrong.');
  await page.locator('canvas').first().waitFor({ timeout: 20_000 });
  await page.waitForTimeout(2_500);
  await capture('report-07-topology-3d.png');

  // Figure 8 - Algorithm catalogue.
  await go('/algorithms');
  await page.getByRole('table').first().waitFor({ timeout: 20_000 });
  await page.waitForTimeout(600);
  await capture('report-08-algorithms.png');

  // Figure 9 - Analytics computed from live API data.
  await go('/analytics');
  await page.getByText('Events / min').waitFor();
  await capture('report-09-analytics.png');

  // Figure 10 - Tablet 1024x768 overview.
  await page.setViewportSize({ width: 1024, height: 768 });
  await go('/', 'Command center');
  await page.locator('.overview-metrics').waitFor();
  await capture('report-10-tablet-1024.png');

  // Figure 11 - Mobile 390x844 overview.
  await page.setViewportSize({ width: 390, height: 844 });
  await go('/', 'Command center');
  await page.locator('.overview-metrics').waitFor();
  await capture('report-11-mobile-390.png');

  console.log(`\nCaptured ${captured.length} report figures in ${output}`);
} finally {
  await page?.close().catch(() => undefined);
  await browser.close();
}