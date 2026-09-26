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

  public static handleGetOffersResponse(respData: Offer[]) {
    const offers: Offer[] = [];
    console.log(offers.toString());

    for (const key in respData) {
      if (Object.hasOwn(respData, key)) {
        offers.push(respData[key]);
      }
    }
    return offers;
  }

  public static handleGetBookingsResponse(respData: Booking[]) {
    const bookings: Booking[] = [];
    console.log(bookings.toString());

    for (const key in respData) {
      if (Object.hasOwn(respData, key)) {
        bookings.push(respData[key]);
      }
    }
    return bookings;
  }

  public static handleMessageResponse(respData: Message[]) {
    const messages: Message[] = [];
    for (const key in respData) {
      if (Object.hasOwn(respData, key)) {

        messages.push(respData[key]);
      }
    }
    console.log(messages);
    return messages;
  }
}
