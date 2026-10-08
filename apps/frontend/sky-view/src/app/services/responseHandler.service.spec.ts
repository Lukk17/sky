import { HttpErrorResponse } from '@angular/common/http';
import { Offer } from './offer.service';
import { ResponseHandlerService } from './responseHandler.service';

describe('ResponseHandlerService', () => {
  it('maps an indexed payload to an offer list', () => {
    const payload = { 0: { id: 1, hotelName: 'Harbor' } } as unknown as Offer[];

    const result = ResponseHandlerService.handleGetOffersResponse(payload);

    expect(result.length).toBe(1);
    expect(result[0].hotelName).toBe('Harbor');
  });

  it('sorts gallery by position and derives cover from position 0', () => {
    const payload = [{ id: 1, hotelName: 'H', gallery: [{ id: 'b', position: 1, url: 'u2', main: false }, { id: 'a', position: 0, url: 'u1', main: false }], coverPhotoUrl: null } as unknown as Offer];

    const result = ResponseHandlerService.handleGetOffersResponse(payload);

    expect(result[0].gallery!.map((p) => p.id)).toEqual(['a', 'b']);
    expect(result[0].coverPhotoUrl).toBe('u1');
  });

  it('prefers the main-flagged photo for cover when it is not first', () => {
    const payload = [{ id: 5, hotelName: 'M', gallery: [{ id: 'a', position: 0, url: 'u1', main: false }, { id: 'b', position: 1, url: 'u2', main: true }], coverPhotoUrl: null } as unknown as Offer];

    const result = ResponseHandlerService.handleGetOffersResponse(payload);

    expect(result[0].coverPhotoUrl).toBe('u2');
    expect(result[0].gallery!.find((p) => p.id === 'b')!.main).toBeTrue();
  });

  it('leaves an empty gallery with a null cover', () => {
    const payload = [{ id: 2, hotelName: 'E' } as unknown as Offer];

    const result = ResponseHandlerService.handleGetOffersResponse(payload);

    expect(result[0].gallery).toEqual([]);
    expect(result[0].coverPhotoUrl).toBeNull();
  });

  it('falls back to legacy photoUrl when gallery is empty', () => {
    const payload = [{ id: 3, hotelName: 'L', photoUrl: 'legacy' } as unknown as Offer];

    const result = ResponseHandlerService.handleGetOffersResponse(payload);

    expect(result[0].coverPhotoUrl).toBe('legacy');
  });

  it('handleError rethrows the http error', (done) => {
    const error = { status: 500, message: 'boom', error: 'boom' } as HttpErrorResponse;

    ResponseHandlerService.handleError(error, 'test()').subscribe({
      next: () => fail('expected no emission'),
      error: (err) => {
        expect(err).toBe(error);
        expect((err as HttpErrorResponse).status).toBe(500);
        done();
      },
    });
  });
});
