import { test, expect } from '@playwright/test';

test.use({ ignoreHTTPSErrors: true });
test.describe.configure({ retries: 1 });

test('session login through the gateway', async ({ page }) => {
  await page.goto('/auth');
  await page.locator('app-auth').getByRole('button', { name: 'Login' }).click();

  await page.locator('#username').fill('lukk');
  await page.locator('#password').fill('local');
  await page.locator('#kc-login').click();

  await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'My Offers' })).toBeVisible();

  await page.getByRole('link', { name: 'My Offers' }).click();
  await expect(page.locator('app-my-offers')).toBeAttached();
  await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();

  await page.getByTestId('nav-home').click();
  await page.getByRole('button', { name: 'Logout' }).click();
  await page.waitForResponse('**/logout');
  await page.goto('/home', { waitUntil: 'commit' }).catch(() => undefined);
  await page.goto('/home');
  await expect(page.getByRole('button', { name: 'Login' }).first()).toBeVisible({ timeout: 30000 });

  const sessionState = await page.evaluate(() =>
    fetch('http://localhost:5777/api/session', { credentials: 'include' }).then((response) => response.status));
  expect(sessionState).toBe(401);
});
