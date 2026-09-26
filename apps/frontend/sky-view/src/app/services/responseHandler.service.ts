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

  public static handleGetOffersResponse(respData: Offer[] | { content?: Offer[] }) {
    const offers = ResponseHandlerService.unwrap<Offer>(respData as Offer[]);
    console.log(offers.toString());
    return offers;
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
