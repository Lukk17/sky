import { test, expect } from '@playwright/test';

interface GalleryPhoto {
  id: string;
  position: number;
  url: string | null;
  main?: boolean;
}

test('live gallery: list reads skip presigning, single reads presign', async ({ request }) => {
  const resp = await request.get('http://localhost:5777/api/v1/offers?size=100');
  expect(resp.ok()).toBeTruthy();
  const body = await resp.json();
  const offers = body.content ?? body;
  expect(offers.length).toBeGreaterThan(0);
  for (const offer of offers as Array<{ id: string; hotelName: string; gallery?: GalleryPhoto[] }>) {
    const gallery = offer.gallery ?? [];
    expect(gallery.length, `${offer.hotelName} lists at least 2 photos`).toBeGreaterThanOrEqual(2);
    const positions = gallery.map((g) => g.position).sort((a, b) => a - b);
    expect(positions, `${offer.hotelName} lists dense positions`).toEqual(
      gallery.map((_, index) => index),
    );
    const mains = gallery.filter((g) => g.main === true);
    expect(mains.length, `${offer.hotelName} lists exactly one main photo`).toBe(1);

    const detail = await request.get(`http://localhost:5777/api/v1/offers/${offer.id}`);
    expect(detail.ok()).toBeTruthy();
    const single = await detail.json();
    const full = (single.gallery ?? []) as GalleryPhoto[];
    expect(full.length, `${offer.hotelName} reads at least 2 photos`).toBeGreaterThanOrEqual(2);
    const singleMains = full.filter((g) => g.main === true);
    expect(singleMains.length, `${offer.hotelName} reads exactly one main photo`).toBe(1);
    for (const photo of full) {
      expect(photo.url, `${offer.hotelName} photo ${photo.position} is presigned`).toBeTruthy();
    }
    expect(singleMains[0].url!.split('?')[0], `${offer.hotelName} main is the hotel exterior`).toContain(
      'exterior',
    );
    const urls = full.map((g) => g.url!.split('?')[0]);
    expect(new Set(urls).size, `${offer.hotelName} holds no duplicate photos`).toBe(urls.length);
    expect(
      single.coverPhotoUrl?.split('?')[0],
      `${offer.hotelName} cover matches main`,
    ).toBe(singleMains[0].url!.split('?')[0]);
  }
});
