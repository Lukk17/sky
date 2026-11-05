import {Injectable} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {Observable, throwError} from 'rxjs';
import {Offer} from './offer.service';
import {Message} from './message.service';
import {Booking} from './booking.service';

@Injectable({
  providedIn: 'root'
})
export class ResponseHandlerService {

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public static handleError(errorResp: HttpErrorResponse, method: string): Observable<never> {
    return throwError(() => errorResp);
  }

  public static normalizeOfferGallery(offer: Offer): Offer {
    const raw = Array.isArray((offer as Offer).gallery) ? [...(offer.gallery as unknown[])] : [];
    const gallery = (raw as Partial<import('./offer.service').OfferPhoto>[])
      .filter((p) => p != null && typeof (p as { url?: unknown }).url === 'string' && ((p as { url: string }).url as string).length > 0)
      .map((p) => ({ id: String((p as { id?: unknown }).id ?? ''), position: Number((p as { position?: unknown }).position ?? 0), url: (p as { url: string }).url, main: (p as { main?: unknown }).main === true }))
      .sort((a, b) => a.position - b.position);
    (offer as Offer).gallery = gallery as import('./offer.service').OfferPhoto[];
    const mainPhoto = gallery.find((g) => g.main) ?? gallery[0];
    const fallback = mainPhoto ? mainPhoto.url : (offer.photoUrl ?? offer.externalPhotoUrl ?? (offer as Offer).photoPath ?? null);
    (offer as Offer).coverPhotoUrl = (offer as Offer).coverPhotoUrl ?? fallback;
    if ((offer as Offer).coverPhotoUrl == null) {
      (offer as Offer).coverPhotoUrl = fallback;
    }
    if (gallery.length === 0 && (offer as Offer).coverPhotoUrl == null) {
      (offer as Offer).coverPhotoUrl = null;
    }
    return offer;
  }

  public static handleGetOffersResponse(respData: Offer[]) {
    respData.forEach((o) => ResponseHandlerService.normalizeOfferGallery(o));
    return respData;
  }

  public static handleGetOfferResponse(respData: Offer): Offer {
    return ResponseHandlerService.normalizeOfferGallery(respData);
  }

  public static handleGetBookingsResponse(respData: Booking[]) {
    return respData;
  }

  public static handleMessageResponse(respData: Message[]) {
    return respData;
  }
}
