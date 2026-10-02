import { test, expect } from '@playwright/test';

const offerId = '123e4567-e89b-12d3-a456-426614174000';
const offer = {
  id: offerId,
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

  await expect(page).toHaveURL(new RegExp(`offerDetails\\?offerId=${offerId}`));
  await expect(page.getByRole('heading', { name: 'Grand Test Hotel' })).toBeVisible();
  await expect(page.getByText('No bookings yet')).toBeVisible();
});

test('offer details render from query param after reload (no service memory)', async ({ page }) => {
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [offer] }));
  await page.route('**/api/v1/user/bookings', (route) => route.fulfill({ json: [] }));

  await page.goto(`/offerDetails?offerId=${offerId}`);
  await expect(page.getByRole('heading', { name: 'Grand Test Hotel' })).toBeVisible();
});

test('bookings list refreshes after booking and 409 shows friendly message', async ({ page }) => {
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [offer] }));
  let bookingsCall = 0;
  await page.route('**/api/v1/user/bookings', (route) => {
    bookingsCall += 1;
    if (bookingsCall === 1) {
      return route.fulfill({ json: [] });
    }
    return route.fulfill({ json: [{ id: 1, offerId, bookedDate: '2026-11-01', bookingUser: 'lukk', ownerEmail: 'owner@test.local' }] });
  });
  await page.route('**/api/v1/bookings', async (route) => {
    if (route.request().method() === 'POST') {
      return route.fulfill({ status: 201, json: {} });
    }
    return route.continue();
  });

  await page.goto(`/offerDetails?offerId=${offerId}`);
  await page.getByTestId('booking-date').fill('2026-11-01');
  await page.getByTestId('booking-submit').locator('button').click();
  await expect(page.getByTestId('booking-row')).toBeVisible();
});

test('duplicate booking date shows friendly taken message without console error', async ({ page }) => {
  const errors: string[] = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      errors.push(msg.text());
    }
  });
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [offer] }));
  await page.route('**/api/v1/user/bookings', (route) => route.fulfill({ json: [] }));
  await page.route('**/api/v1/bookings', async (route) => {
    if (route.request().method() === 'POST') {
      return route.fulfill({ status: 409, json: { title: 'Conflict' } });
    }
    return route.continue();
  });

  await page.goto(`/offerDetails?offerId=${offerId}`);
  await page.getByTestId('booking-date').fill('2026-11-01');
  await page.getByTestId('booking-submit').locator('button').click();
  await expect(page.getByTestId('booking-error')).toContainText('already taken');
  expect(errors.filter((t) => t.includes('ERROR') || t.includes('Error in'))).toEqual([]);
});
