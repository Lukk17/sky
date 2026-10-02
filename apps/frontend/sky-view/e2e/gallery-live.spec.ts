import { test, expect } from '@playwright/test';

test('live gallery: every offer holds an exterior main photo and cover matches it', async ({ request }) => {
  const resp = await request.get('http://localhost:5777/api/v1/offers?size=100');
  expect(resp.ok()).toBeTruthy();
  const body = await resp.json();
  const offers = body.content ?? body;
  expect(offers.length).toBeGreaterThan(0);
  for (const offer of offers) {
    const gallery = offer.gallery ?? [];
    expect(gallery.length, `${offer.hotelName} holds at least 2 photos`).toBeGreaterThanOrEqual(2);
    const mains = gallery.filter((g: { main?: boolean }) => g.main === true);
    expect(mains.length, `${offer.hotelName} holds exactly one main photo`).toBe(1);
    expect(mains[0].url.split('?')[0], `${offer.hotelName} main is the hotel exterior`).toContain('exterior');
    const urls = gallery.map((g: { url: string }) => g.url.split('?')[0]);
    expect(new Set(urls).size, `${offer.hotelName} holds no duplicate photos`).toBe(urls.length);
    expect(offer.coverPhotoUrl?.split('?')[0], `${offer.hotelName} cover matches main`).toBe(mains[0].url.split('?')[0]);
  }
});
