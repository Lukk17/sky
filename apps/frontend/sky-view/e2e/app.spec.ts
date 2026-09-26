import { test, expect } from '@playwright/test';

test('home route serves the application', async ({ page }) => {
  await page.goto('/home');
  await expect(page.locator('app-root')).toBeAttached();
});
