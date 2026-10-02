import { test, expect } from '@playwright/test';

const offer = {
  id: 1,
  hotelName: 'Grand Test Hotel',
  description: 'A lovely place',
  comment: 'Great stay',
  price: 199,
  ownerEmail: 'owner@test.local',
  roomCapacity: 2,
  city: 'Warsaw',
  country: 'Poland',
  photoPath: '',
  coverPhotoUrl: null,
  gallery: [],
};

test('clicking an offer card opens details without crashing', async ({ page }) => {
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [offer] }));
  await page.route('**/api/v1/user/bookings', (route) => route.fulfill({ json: [] }));

  await page.goto('/');
  await page.getByText('Grand Test Hotel').first().click();

  await expect(page).toHaveURL(/offerDetails\?offerId=1/);
  await expect(page.getByRole('heading', { name: 'Grand Test Hotel' })).toBeVisible();
  await expect(page.getByText('No bookings yet')).toBeVisible();
});

test('offer details render from query param after reload (no service memory)', async ({ page }) => {
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [offer] }));
  await page.route('**/api/v1/user/bookings', (route) => route.fulfill({ json: [] }));

  await page.goto('/offerDetails?offerId=1');
  await expect(page.getByRole('heading', { name: 'Grand Test Hotel' })).toBeVisible();
});
