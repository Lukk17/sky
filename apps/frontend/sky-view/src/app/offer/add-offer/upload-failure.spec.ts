import { MAX_PHOTO_BYTES } from './add-offer.component';

describe('upload failure banner', () => {
  it('caps photo size at 5 MB', () => {
    expect(MAX_PHOTO_BYTES).toBe(5 * 1024 * 1024);
  });
});
