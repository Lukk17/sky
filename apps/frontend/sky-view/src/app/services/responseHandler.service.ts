import {Injectable} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {EMPTY, Observable} from 'rxjs';
import {Offer} from './offer.service';
import {Message} from './message.service';
import {Booking} from './booking.service';

@Injectable({
  providedIn: 'root'
})
export class ResponseHandlerService {

  public static handleError(errorResp: HttpErrorResponse, method: string): Observable<never> {
    console.log(`Error in Offer service method: ${method} with status: ${errorResp.status}
    with message: ${JSON.stringify(errorResp.message)} with error: ${JSON.stringify(errorResp.error)}`);

    return EMPTY;
  }

  private static unwrap<T>(respData: T[] | { content?: T[] } | Record<string, T>): T[] {
    if (Array.isArray(respData)) {
      return [...respData];
    }
    if (respData != null && Array.isArray((respData as { content?: unknown }).content)) {
      return [...((respData as { content: T[] }).content)];
    }
    const items: T[] = [];
    for (const key in respData) {
      if (Object.hasOwn(respData, key)) {
        items.push((respData as Record<string, T>)[key]);
      }
    }
    return items;
  }

  public static normalizeOfferGallery(offer: Offer): Offer {
    const raw = Array.isArray((offer as Offer).gallery) ? [...(offer.gallery as unknown[])] : [];
    const gallery = (raw as Partial<import('./offer.service').OfferPhoto>[])
      .filter((p) => p != null && typeof (p as { url?: unknown }).url === 'string' && ((p as { url: string }).url as string).length > 0)
      .map((p) => ({ id: String((p as { id?: unknown }).id ?? ''), position: Number((p as { position?: unknown }).position ?? 0), url: (p as { url: string }).url }))
      .sort((a, b) => a.position - b.position);
    (offer as Offer).gallery = gallery as import('./offer.service').OfferPhoto[];
    const fallback = gallery.length > 0 ? gallery[0].url : (offer.photoUrl ?? offer.externalPhotoUrl ?? (offer as Offer).photoPath ?? null);
    (offer as Offer).coverPhotoUrl = (offer as Offer).coverPhotoUrl ?? fallback;
    if ((offer as Offer).coverPhotoUrl == null) {
      (offer as Offer).coverPhotoUrl = fallback;
    }
    if (gallery.length === 0 && (offer as Offer).coverPhotoUrl == null) {
      (offer as Offer).coverPhotoUrl = null;
    }
    return offer;
  }

  public static handleGetOffersResponse(respData: Offer[] | { content?: Offer[] }) {
    const offers = ResponseHandlerService.unwrap<Offer>(respData as Offer[]);
    offers.forEach((o) => ResponseHandlerService.normalizeOfferGallery(o));
    console.log(offers.toString());
    return offers;
  }

  public static handleGetOfferResponse(respData: Offer): Offer {
    return ResponseHandlerService.normalizeOfferGallery(respData);
  }

  public static handleGetBookingsResponse(respData: Booking[] | { content?: Booking[] }) {
    const bookings = ResponseHandlerService.unwrap<Booking>(respData as Booking[]);
    console.log(bookings.toString());
    return bookings;
  }

  public static handleMessageResponse(respData: Message[] | { content?: Message[] }) {
    const messages = ResponseHandlerService.unwrap<Message>(respData as Message[]);
    console.log(messages);
    return messages;
  }
}
