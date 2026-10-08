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
  // Logout is a full-browser form POST through the edge /logout endpoint,
  // which clears the server-side session (tokens never touch the browser)
  // and redirects back to the app home. The browser never navigates to the
  // Keycloak end-session endpoint without an id_token_hint, so no logout
  // confirm page can appear.
  await page.getByRole('button', { name: 'Logout' }).click();
  await page.waitForURL(/\/home/, { timeout: 30000 });
  await expect(page.getByRole('button', { name: 'Login' }).first()).toBeVisible({ timeout: 30000 });

  const sessionState = await page.evaluate(() =>
    fetch('http://localhost:5777/api/session', { credentials: 'include' }).then((response) => response.status));
  expect(sessionState).toBe(401);

  // Immediate re-login must land straight on the Keycloak form with zero
  // extra clicks and zero confirm page: the SSO session died at logout.
  await page.goto('/auth');
  await page.locator('app-auth').getByRole('button', { name: 'Login' }).click();
  await expect(page.locator('#username')).toBeVisible({ timeout: 30000 });
  await expect(page.locator('text=Logging out')).toHaveCount(0);
});
