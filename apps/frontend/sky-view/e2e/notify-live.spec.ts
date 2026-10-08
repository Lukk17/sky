import { test, expect, Browser } from '@playwright/test';

test.use({ ignoreHTTPSErrors: true });
test.describe.configure({ retries: 0 });

async function loginAs(browser: Browser, username: string): Promise<{ close: () => Promise<void>, page: any }> {
  const context = await browser.newContext({ ignoreHTTPSErrors: true });
  const page = await context.newPage();
  await page.goto('/auth');
  await page.locator('app-auth').getByRole('button', { name: 'Login' }).click();
  await page.locator('#username').fill(username);
  await page.locator('#password').fill('local');
  await page.locator('#kc-login').click();
  await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible({ timeout: 30000 });
  return { close: () => context.close(), page };
}

test('live notify: banner, badge and chat update across two sessions without refresh', async ({ browser }) => {
  const receiverText = `live-probe-${Date.now()}`;
  const alice = await loginAs(browser, 'lukk');
  const bob = await loginAs(browser, 'traveler');
  try {
    await bob.page.goto('/messages');
    await expect(bob.page.getByTestId('thread-list')).toBeVisible({ timeout: 30000 });

    await alice.page.goto('/messages');
    await expect(alice.page.getByTestId('thread-list')).toBeVisible({ timeout: 30000 });
    await alice.page.getByTestId('thread-traveler@sky.dev').click();
    await alice.page.getByTestId('reply-input').fill(receiverText);
    await alice.page.getByTestId('reply-send').click();
    await expect(alice.page.getByTestId('conversation')).toContainText(receiverText, { timeout: 30000 });

    await expect(bob.page.getByTestId('notify-banner')).toContainText(receiverText, { timeout: 30000 });
    await expect(bob.page.getByTestId('notify-badge')).toBeVisible({ timeout: 30000 });
    await expect(bob.page.getByTestId('conversation')).toContainText(receiverText, { timeout: 30000 });
  } finally {
    await alice.close();
    await bob.close();
  }
});
