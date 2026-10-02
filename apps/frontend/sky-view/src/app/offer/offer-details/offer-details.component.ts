import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {CalendarEvent} from 'angular-calendar';
import {Offer, OfferService} from '../../services/offer.service';
import {SkyAuthService} from '../../services/sky-auth.service';
import {Location} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {Booking, BookingService} from '../../services/booking.service';
import {NgForm} from '@angular/forms';

const BOOKED_COLOR = {primary: '#ef4444', secondary: '#fecaca'};

@Component({
    selector: 'app-offer-details',
    templateUrl: './offer-details.component.html',
    styleUrls: ['./offer-details.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class OfferDetailsComponent implements OnInit {

  offer: Offer | null = null;
  bookings: Booking[] = [];
  events: CalendarEvent[] = [];
  viewDate: Date = new Date();
  isOwner = false;
  isLoggedIn = false;
  selectedPhotoUrl: string | null = null;
  loadError: string | null = null;

  coverUrl(): string | null {
    return this.selectedPhotoUrl ?? this.offer?.coverPhotoUrl ?? this.offer?.gallery?.[0]?.url ?? this.offer?.photoUrl ?? null;
  }

  selectPhoto(url: string): void {
    this.selectedPhotoUrl = url;
  }

  constructor(private offerService: OfferService, private auth: SkyAuthService, private bookingService: BookingService,
              private location: Location, private router: Router, private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.offer = this.offerService.detailedOffer ?? null;
    this.auth.currentUser$.subscribe((email) => {
      this.isLoggedIn = email != null;
      this.isOwner = email != null && this.offer?.ownerEmail === email;
      if (this.isLoggedIn) {
        this.getBookings();
      } else {
        this.bookings = [];
        this.events = [];
      }
    });
    this.route.queryParams.subscribe((params) => {
      const id = String(params['offerId'] ?? '');
      if (id.length > 0 && this.offer?.id !== id) {
        this.loadOfferById(id);
      }
    });
  }

  editOffer(offer: Offer) {
    this.offerService.editedOffer = offer;
    this.router.navigate(['/editOffer']).then();
  }

  deleteOffer(id: string) {
    this.offerService.deleteOffer(id).subscribe(value => {
      console.log(value);
      this.router.navigate(['/myOffers']).then();
      return value;
    });
  }

  private getBookings() {
    this.bookingService.getBookedOffers().subscribe(bookings => {
      this.bookings = bookings ?? [];
      this.events = this.bookings.map((b) => ({
        start: new Date(b.bookedDate),
        title: `Booked by ${b.bookingUser}`,
        color: BOOKED_COLOR,
        allDay: true,
      }));
    });
  }

  bookingError: string | null = null;

  private loadOfferById(id: string) {
    this.offerService.getAllOffers().subscribe(offers => {
      const found = (offers ?? []).find((o) => o.id === id);
      if (found) {
        this.offer = found;
        this.loadError = null;
      } else if (!this.offer) {
        this.loadError = 'Offer not found';
      }
    });
  }

  deleteBooking(id: number) {
    if (!this.isLoggedIn) {
      this.auth.login(this.router.url);
      return;
    }
    this.bookingService.deleteBooking(id).subscribe(value => {
      console.log(value);
      this.getBookings();
      return value;
    });
  }

  onSubmit(bookingForm: NgForm, offer: Offer | null) {
    if (!offer) {
      return;
    }
    if (!this.isLoggedIn) {
      this.auth.login(this.router.url);
      return;
    }
    this.bookingError = null;
    this.bookingService.addBooking(bookingForm, offer).subscribe({
      next: () => {
        this.getBookings();
      },
      error: (err: { status?: number }) => {
        if (err?.status === 401) {
          this.auth.login(this.router.url);
        } else if (err?.status === 409) {
          this.bookingError = 'This date is already taken. Please choose another date.';
        } else {
          this.bookingError = 'Booking failed. Please try again.';
        }
      },
    });
  }
}
