import {Injectable} from '@angular/core';
import {catchError, map} from 'rxjs/operators';
import {ResponseHandlerService} from './responseHandler.service';
import {Offer} from './offer.service';
import {BookingsService} from '../api-generated/api/bookings.service';
import {BookingDTO} from '../api-generated/model/bookingDTO';
import {BookingPage} from '../api-generated/model/bookingPage';
import {Pageable} from '../api-generated/model/pageable';


@Injectable({
  providedIn: 'root'
})
export class BookingService {

  constructor(private bookingsService: BookingsService) {
  }

  private static mapBookingDtoToBooking(dto: BookingDTO): Booking {
    return new Booking(dto.id, dto.offerId, dto.bookedDate, dto.bookingUser, dto.ownerEmail);
  }

  public getBookedOffers() {
    const pageable: Pageable = {} as Pageable;
    return this.bookingsService.getBookedOffers(pageable, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'getBookedOffers()')),
      map((resp: BookingPage) => ResponseHandlerService.handleGetBookingsResponse(
        resp.content.map((dto) => BookingService.mapBookingDtoToBooking(dto))))
    );
  }

  deleteBooking(id: string) {
    return this.bookingsService.removeBooking(id, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'deleteBooking()')),
      map(() => undefined)
    );
  }

  addBooking(offer: Offer, dateToBook: string) {
    const bookingPayload = new BookingPayload(String(offer.id), dateToBook);
    return this.bookingsService.bookOffer(bookingPayload, 'body', false).pipe(
      catchError((err) => ResponseHandlerService.handleError(err, 'addBooking()')),
      map((resp: BookingDTO) => BookingService.mapBookingDtoToBooking(resp))
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
  'id': string;
  'offerId': string;
  'bookedDate': string;
  'bookingUser': string;
  'ownerEmail': string;

  constructor(id: string, offerId: string, bookedDate: string, bookingUser: string, ownerEmail: string) {
    this.id = id;
    this.offerId = offerId;
    this.bookedDate = bookedDate;
    this.bookingUser = bookingUser;
    this.ownerEmail = ownerEmail;
  }
}

export class PersonalBooking {
  'hotelName': string;
  'id': string;
  'offerId': string;
  'date': string;

  constructor(hotelName: string, id: string, offerId: string, date: string) {
    this.hotelName = hotelName;
    this.id = id;
    this.date = date;
    this.offerId = offerId;
  }
}
