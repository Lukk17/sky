import {Injectable} from '@angular/core';
import {catchError, map} from 'rxjs/operators';
import {ResponseHandlerService} from './responseHandler.service';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {Offer} from './offer.service';


@Injectable({
  providedIn: 'root'
})
export class BookingService {

  private BASE_ADDRESS = `${environment.apiBaseUrl}`;
  private BOOKINGS = this.BASE_ADDRESS + `${environment.bookings}`;
  private ADD_BOOKING = this.BASE_ADDRESS + `${environment.addBooking}`;
  private DELETE_BOOKING = this.BASE_ADDRESS + `${environment.deleteBooking}`;

  constructor(private http: HttpClient) {
  }

  private static buildBookingPayload(offer: Offer, dateToBook: string) {
    return new BookingPayload(String(offer.id), dateToBook);
  }

  public getBookedOffers() {
    return this.http
      .get<Booking[]>(this.BOOKINGS)
      .pipe(
        catchError((err) => ResponseHandlerService.handleError(err, 'getBookedOffers()')),
        map((resp) => ResponseHandlerService.handleGetBookingsResponse(resp))
      );
  }

  deleteBooking(id: number) {
    return this.http.delete<void>(this.DELETE_BOOKING + id)
      .pipe(
        catchError((err) => ResponseHandlerService.handleError(err, 'deleteOffer()'))
      );
  }

  addBooking(offer: Offer, dateToBook: string) {
    const bookingPayload = BookingService.buildBookingPayload(offer, dateToBook);
    return this.http.post<Booking>(this.ADD_BOOKING,
      bookingPayload).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'addBooking()'))
    );
  }
}

export class BookingPayload {
  'offerId': string;
  'dateToBook': string;

  constructor(offerId: string, dateToBook: string) {
    this.offerId = offerId;
    this.dateToBook = dateToBook;
  }
}

export class Booking {
  'id': number;
  'offerId': string;
  'bookedDate': string;
  'bookingUser': string;
  'ownerEmail': string;

  constructor(id: number, offerId: string, bookedDate: string, bookingUser: string, ownerEmail: string) {
    this.id = id;
    this.offerId = offerId;
    this.bookedDate = bookedDate;
    this.bookingUser = bookingUser;
    this.ownerEmail = ownerEmail;
  }
}

export class PersonalBooking {
  'hotelName': string;
  'id': number;
  'offerId': string;
  'date': string;

  constructor(hotelName: string, id: number, offerId: string, date: string) {
    this.hotelName = hotelName;
    this.id = id;
    this.date = date;
    this.offerId = offerId;
  }
}
