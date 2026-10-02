import { test, expect } from '@playwright/test';

test('add-offer requires a photo file then creates and uploads', async ({ page }) => {
  await page.goto('/addOffer');
  await expect(page.getByTestId('add-submit').locator('button')).toBeDisabled();

  await page.getByTestId('add-hotel').fill('Grand Test Hotel');
  await expect(page.getByTestId('add-submit').locator('button')).toBeDisabled();

  await page.getByTestId('add-description').fill('A lovely place');
  await page.getByTestId('add-city').fill('Warsaw');
  await page.getByTestId('add-country').fill('Poland');
  await page.getByTestId('add-capacity').fill('2');
  await page.getByTestId('add-price').fill('199');

  await expect(page.getByTestId('add-submit').locator('button')).toBeDisabled();
  await expect(page.getByTestId('add-photo-hint')).toBeVisible();

  await page.getByTestId('add-photo-file').setInputFiles({
    name: 'room.png',
    mimeType: 'image/png',
    buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64'),
  });
  await expect(page.getByTestId('add-photo-filename')).toContainText('room.png');
  await expect(page.getByTestId('add-submit').locator('button')).toBeEnabled();

  await page.route('**/api/v1/owner/offers', async (route) => {
    if (route.request().method() === 'POST') {
      return route.fulfill({ status: 201, json: { id: '123e4567-e89b-12d3-a456-426614174000' } });
    }
    return route.continue();
  });
  const postPromise = page.waitForResponse(
    (resp) => resp.url().includes('/api/v1/owner/offers') && resp.request().method() === 'POST',
  );
  await page.getByTestId('add-submit').locator('button').click();
  await postPromise;
});

test('add-offer uploads a photo file after creating the offer', async ({ page }) => {
  await page.goto('/addOffer');
  await page.getByTestId('add-hotel').fill('Grand Test Hotel');
  await page.getByTestId('add-description').fill('A lovely place');
  await page.getByTestId('add-city').fill('Warsaw');
  await page.getByTestId('add-country').fill('Poland');
  await page.getByTestId('add-capacity').fill('2');
  await page.getByTestId('add-price').fill('199');

  await page.getByTestId('add-photo-file').setInputFiles({
    name: 'room.png',
    mimeType: 'image/png',
    buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64'),
  });
  await expect(page.getByTestId('add-photo-filename')).toContainText('room.png');

  await page.route('**/api/v1/owner/offers', async (route) => {
    if (route.request().method() === 'POST' && !route.request().url().includes('/photo')) {
      return route.fulfill({ status: 201, json: { id: '123e4567-e89b-12d3-a456-426614174000' } });
    }
    return route.continue();
  });
  await page.route('**/api/v1/owner/offers/*/photo', async (route) => {
    return route.fulfill({ status: 200, json: { id: '123e4567-e89b-12d3-a456-426614174000' } });
  });
  const uploadPromise = page.waitForResponse(
    (resp) => resp.url().includes('/photo') && resp.request().method() === 'POST',
  );
  await page.getByTestId('add-submit').locator('button').click();
  await uploadPromise;
});
