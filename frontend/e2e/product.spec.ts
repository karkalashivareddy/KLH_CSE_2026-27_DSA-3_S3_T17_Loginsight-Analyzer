import { expect, test } from '@playwright/test';

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
  await expect(page.getByRole('figure', { name: 'FROM EVENT TO EVIDENCE' })).toBeVisible();
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
  await analyticsTabs.evaluate((element) => {
    element.classList.add('atlas-scroll-revealed');
  });
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
