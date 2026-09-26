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

  it('handleError completes without emitting', (done) => {
    const error = { status: 500, message: 'boom', error: 'boom' } as HttpErrorResponse;

    ResponseHandlerService.handleError(error, 'test()').subscribe({
      next: () => fail('expected no emission'),
      complete: () => done(),
    });
  });
});
