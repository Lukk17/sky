import {of} from 'rxjs';
import {BookingsService} from '../api-generated/api/bookings.service';
import {BookingDTO} from '../api-generated/model/bookingDTO';
import {BookingPage} from '../api-generated/model/bookingPage';
import {Booking, BookingService} from './booking.service';
import {Offer} from './offer.service';

function bookingPage(content: BookingDTO[]): BookingPage {
  return {
    content,
    empty: content.length === 0,
    first: true,
    last: true,
    number: 0,
    numberOfElements: content.length,
    pageable: {
      offset: 0,
      pageNumber: 0,
      pageSize: 20,
      paged: true,
      sort: {empty: true, sorted: false, unsorted: true},
      unpaged: false,
    },
    size: 20,
    sort: {empty: true, sorted: false, unsorted: true},
    totalElements: content.length,
    totalPages: 1,
  };
}

const bookingDto: BookingDTO = {
  id: '22222222-2222-2222-2222-222222222222',
  offerId: '11111111-1111-1111-1111-111111111111',
  bookedDate: '2026-11-10',
  bookingUser: 'user@test.local',
  ownerEmail: 'owner@test.local',
};

describe('BookingService', () => {
  it('maps the typed booking page content to bookings', () => {
    const getBookedOffers = jasmine.createSpy('getBookedOffers').and.returnValue(of(bookingPage([bookingDto])));
    const bookingsService = {getBookedOffers} as unknown as BookingsService;
    const service = new BookingService(bookingsService);

    let bookings: Booking[] = [];
    service.getBookedOffers().subscribe((result) => (bookings = result));

    expect(getBookedOffers).toHaveBeenCalledOnceWith(jasmine.any(Object), 'body', false);
    expect(bookings.length).toBe(1);
    expect(bookings[0].id).toBe(bookingDto.id);
    expect(bookings[0].bookedDate).toBe('2026-11-10');
    expect(bookings[0].bookingUser).toBe('user@test.local');
    expect(bookings[0].ownerEmail).toBe('owner@test.local');
  });

  it('deletes through the generated endpoint with the booking id', () => {
    const removeBooking = jasmine.createSpy('removeBooking').and.returnValue(of(undefined));
    const bookingsService = {removeBooking} as unknown as BookingsService;
    const service = new BookingService(bookingsService);

    service.deleteBooking(bookingDto.id).subscribe();

    expect(removeBooking).toHaveBeenCalledOnceWith(bookingDto.id, 'body', false);
  });

  it('books through the generated endpoint and maps the returned booking', () => {
    const bookOffer = jasmine.createSpy('bookOffer').and.returnValue(of(bookingDto));
    const bookingsService = {bookOffer} as unknown as BookingsService;
    const service = new BookingService(bookingsService);
    const offer = {id: bookingDto.offerId} as Offer;

    let booking: Booking | undefined;
    service.addBooking(offer, '2026-11-10').subscribe((result) => (booking = result));

    expect(bookOffer).toHaveBeenCalledTimes(1);
    const [payload, observe, reportProgress] = bookOffer.calls.mostRecent().args;
    expect(payload).toEqual(jasmine.objectContaining({
      offerId: bookingDto.offerId,
      dateToBook: '2026-11-10',
    }));
    expect(observe).toBe('body');
    expect(reportProgress).toBeFalse();
    expect(booking!.bookedDate).toBe('2026-11-10');
  });
});
