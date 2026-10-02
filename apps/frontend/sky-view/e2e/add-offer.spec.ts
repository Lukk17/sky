import { test, expect } from '@playwright/test';

test('add-offer validation blocks invalid submit then creates with photo preview', async ({ page }) => {
  await page.goto('/addOffer');
  await expect(page.getByTestId('add-submit').locator('button')).toBeDisabled();

  await page.getByTestId('add-hotel').fill('Grand Test Hotel');
  await expect(page.getByTestId('add-submit').locator('button')).toBeDisabled();

  await page.getByTestId('add-description').fill('A lovely place');
  await page.getByTestId('add-city').fill('Warsaw');
  await page.getByTestId('add-country').fill('Poland');
  const photoUrl = 'https://example.com/photo.jpg';
  await page.getByTestId('add-photo').fill(photoUrl);
  await page.getByTestId('add-capacity').fill('2');
  await page.getByTestId('add-price').fill('199');

  await expect(page.getByTestId('add-photo-preview')).toBeVisible();
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
