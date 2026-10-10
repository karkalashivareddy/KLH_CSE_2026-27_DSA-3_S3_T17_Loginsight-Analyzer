import { mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';
import { chromium } from '@playwright/test';

const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://127.0.0.1:5173';
const apiBaseURL = process.env.PLAYWRIGHT_API_BASE_URL ?? 'http://127.0.0.1:8080';
const output = resolve(process.cwd(), '../docs/images/signal-atlas');
const browserPath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH;

const dataset = await fetch(`${apiBaseURL}/api/datasets/demo`, { method: 'POST' });
if (!dataset.ok) throw new Error(`Could not load deterministic demo data (${dataset.status}).`);
await mkdir(output, { recursive: true });
const browser = await chromium.launch({ headless: true, ...(browserPath ? { executablePath: browserPath } : {}) });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 });
  const capture = async (name) => page.screenshot({ path: resolve(output, name), animations: 'disabled' });

  await page.goto(new URL('/', baseURL).href);
  await page.getByRole('heading', { name: 'Command center' }).waitFor();
  await page.locator('.signal-pipeline').waitFor();
  await capture('command-center-desktop.png');

  await page.goto(new URL('/logs', baseURL).href);
  await page.getByRole('heading', { name: 'Log explorer' }).waitFor();
  await page.getByRole('searchbox', { name: 'Search logs' }).fill('level:ERROR');
  await page.getByRole('button', { name: 'Search', exact: true }).click();
  await page.getByRole('list', { name: 'Filtered log events' }).waitFor();
  await capture('log-explorer-desktop.png');

  await page.goto(new URL('/services', baseURL).href);
  await page.getByRole('heading', { name: 'Observed service map' }).waitFor();
  await page.locator('.topology-node').first().waitFor();
  await capture('topology-2d-desktop.png');

  await page.goto(new URL('/analytics', baseURL).href);
  await page.getByRole('heading', { name: 'Analytics', exact: true }).waitFor();
  await page.getByText('Events / min').waitFor();
  await capture('analytics-desktop.png');

  await page.goto(new URL('/incidents', baseURL).href);
  await page.getByRole('heading', { name: 'Incident investigations' }).waitFor();
  await page.locator('.incident-investigation-card').waitFor();
  await capture('incident-windows-desktop.png');

  await page.addInitScript(() => {
    const original = HTMLCanvasElement.prototype.getContext;
    HTMLCanvasElement.prototype.getContext = function (context, ...args) {
      if (String(context).includes('webgl')) return null;
      return original.call(this, context, ...args);
    };
  });
  await page.goto(new URL('/services', baseURL).href);
  await page.getByRole('heading', { name: 'Observed service map' }).waitFor();
  await page.getByRole('button', { name: '3D WebGL' }).click();
  await page.getByText('3D visualization unavailable on this device.').waitFor();
  await capture('topology-webgl-fallback-desktop.png');

  await page.goto(new URL('/', baseURL).href);
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.getByRole('button', { name: /Start guided demo/ }).click();
  await page.getByRole('button', { name: /Begin walkthrough/ }).click();
  await page.getByText(/Product explanation/).waitFor();
  await capture('guided-presentation-desktop.png');
  await page.keyboard.press('Escape');

  await page.goto(new URL('/', baseURL).href);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole('heading', { name: 'Command center' }).waitFor();
  await capture('command-center-mobile.png');

  await page.getByRole('button', { name: /Start guided demo/ }).click();
  await page.getByRole('button', { name: /Begin walkthrough/ }).click();
  await page.getByText(/Product explanation/).waitFor();
  await capture('guided-presentation-mobile.png');
  console.log(`Captured nine current-application screenshots in ${output}`);
} finally {
  await browser.close();
}
