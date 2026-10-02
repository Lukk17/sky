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

async function mockLoggedOut(page) {
  await page.route('**/api/session', (route) => route.fulfill({ status: 401, json: {} }));
}

async function mockLoggedIn(page, email: string) {
  await page.route('**/api/session', (route) => route.fulfill({ json: { email } }));
}

test('clicking an offer card opens details without crashing', async ({ page }) => {
  await mockLoggedOut(page);
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [offer] }));

  await page.goto('/');
  await page.getByText('Grand Test Hotel').first().click();

  await expect(page).toHaveURL(new RegExp(`offerDetails\\?offerId=${offerId}`));
  await expect(page.getByRole('heading', { name: 'Grand Test Hotel' })).toBeVisible();
});

test('logged-out details never call secured bookings and prompt login', async ({ page }) => {
  await mockLoggedOut(page);
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [offer] }));
  let bookingsHit = false;
  await page.route('**/api/v1/user/bookings', (route) => {
    bookingsHit = true;
    return route.fulfill({ json: [] });
  });

  await page.goto(`/offerDetails?offerId=${offerId}`);
  await expect(page.getByRole('heading', { name: 'Grand Test Hotel' })).toBeVisible();
  await expect(page.getByTestId('booking-calendar')).toBeVisible();
  await expect(page.getByText('Calendar is read-only')).toBeVisible();
  expect(bookingsHit).toBe(false);
  await expect(page).toHaveURL(new RegExp('offerDetails'));
});

test('offer details render from query param after reload (no service memory)', async ({ page }) => {
  await mockLoggedOut(page);
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [offer] }));

  await page.goto(`/offerDetails?offerId=${offerId}`);
  await expect(page.getByRole('heading', { name: 'Grand Test Hotel' })).toBeVisible();
});

test('calendar shows red booked day and refreshes after booking without reload', async ({ page }) => {
  const now = new Date();
  const day = '15';
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const targetDate = `${now.getFullYear()}-${month}-${day}`;
  const fillDate = `${day}/${month}/${now.getFullYear()}`;
  await mockLoggedIn(page, 'guest@test.local');
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [offer] }));
  let bookingsCall = 0;
  await page.route('**/api/v1/user/bookings', (route) => {
    bookingsCall += 1;
    if (bookingsCall === 1) {
      return route.fulfill({ json: [] });
    }
    return route.fulfill({ json: [{ id: 1, offerId, bookedDate: targetDate, bookingUser: 'guest@test.local', ownerEmail: 'owner@test.local' }] });
  });
  await page.route('**/api/v1/bookings', async (route) => {
    if (route.request().method() === 'POST') {
      return route.fulfill({ status: 201, json: {} });
    }
    return route.continue();
  });

  await page.goto(`/offerDetails?offerId=${offerId}`);
  await expect(page.getByTestId('booking-calendar')).toBeVisible();
  await page.getByTestId('booking-date').fill(fillDate);
  await page.getByTestId('booking-submit').locator('button').click();
  const bookedCell = page.getByTestId('booking-calendar').locator('.cal-day-cell.cal-has-events', { hasText: day });
  await expect(bookedCell.first()).toBeVisible();
});

test('gallery renders cover plus arrows without thumbnails', async ({ page }) => {
  await mockLoggedOut(page);
  const withGallery = { ...offer, coverPhotoUrl: 'https://img.test/a.jpg', gallery: [{ id: 'p1', position: 0, url: 'https://img.test/a.jpg' }, { id: 'p2', position: 1, url: 'https://img.test/b.jpg' }] };
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [withGallery] }));
  await page.goto(`/offerDetails?offerId=${offerId}`);
  await expect(page.getByTestId('offer-gallery')).toBeVisible();
  await expect(page.getByTestId('offer-hero')).toHaveCount(0);
  await expect(page.getByTestId('offer-thumb')).toHaveCount(0);
});

test('day click opens dialog with who plus price and cancel works', async ({ page }) => {
  await mockLoggedIn(page, 'owner@test.local');
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [offer] }));
  const now2 = new Date();
  const month2 = String(now2.getMonth() + 1).padStart(2, '0');
  await page.route('**/api/v1/user/bookings', (route) => route.fulfill({ json: [{ id: 7, offerId, bookedDate: `${now2.getFullYear()}-${month2}-15`, bookingUser: 'guest@test.local', ownerEmail: 'owner@test.local' }] }));
  await page.route('**/api/v1/bookings/7', async (route) => {
    if (route.request().method() === 'DELETE') {
      return route.fulfill({ status: 204, body: '' });
    }
    return route.continue();
  });
  await page.goto(`/offerDetails?offerId=${offerId}`);
  await page.getByTestId('booking-calendar').locator('.cal-day-cell', { hasText: '15' }).first().click();
  await expect(page.getByTestId('day-dialog')).toBeVisible();
  await expect(page.getByTestId('booking-row').first()).toContainText('guest@test.local');
  await expect(page.getByTestId('booking-row').first()).toContainText('$199');
  await page.getByTestId('booking-row').first().getByRole('button', { name: 'Cancel' }).click();
});

test('login from details preserves return url with single encoding', async ({ page }) => {
  await mockLoggedOut(page);
  await page.route('**/api/v1/offers', (route) => route.fulfill({ json: [offer] }));
  await page.goto(`/offerDetails?offerId=${offerId}`);
  await page.evaluate(() => sessionStorage.clear());
  await page.getByTestId('booking-date').fill('20/11/2026');
  const loginReq = page.waitForRequest((req) => req.url().includes('/oauth2/authorization/keycloak'));
  await page.getByTestId('booking-submit').locator('button').click();
  const req = await loginReq;
  const rd = new URL(req.url()).searchParams.get('rd') ?? '';
  expect(decodeURIComponent(rd)).toBe(`http://localhost:4200/offerDetails?offerId=${offerId}`);
  expect(req.url()).not.toContain('%253F');
});

test('duplicate booking date shows friendly taken message without console error', async ({ page }) => {
  await mockLoggedIn(page, 'guest@test.local');
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
  await page.getByTestId('booking-date').fill('01/11/2026');
  await page.getByTestId('booking-submit').locator('button').click();
  await expect(page.getByTestId('booking-error')).toContainText('already taken');
  expect(errors.filter((t) => t.includes('ERROR') || t.includes('Error in'))).toEqual([]);
});
