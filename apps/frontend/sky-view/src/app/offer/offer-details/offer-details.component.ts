import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {CalendarEvent} from 'angular-calendar';
import {Offer, OfferService} from '../../services/offer.service';
import {SkyAuthService} from '../../services/sky-auth.service';
import {Location} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {Booking, BookingService} from '../../services/booking.service';
import {NgForm} from '@angular/forms';

const BOOKED_COLOR = {primary: '#ef4444', secondary: '#7f1d1d'};

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
  dayDialogVisible = false;
  selectedDayLabel = '';
  selectedDayBookings: Booking[] = [];
  bookingDateText = '';
  galleriaResponsive = [{breakpoint: '1024px', numVisible: 5}, {breakpoint: '768px', numVisible: 3}];

  coverUrl(): string | null {
    return this.selectedPhotoUrl ?? this.offer?.coverPhotoUrl ?? this.offer?.gallery?.[0]?.url ?? this.offer?.photoUrl ?? null;
  }

  galleryUrls(): string[] {
    return (this.offer?.gallery ?? []).map((g) => g.url).filter((u) => !!u);
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
      this.router.navigate(['/myOffers']).then();
      return value;
    });
  }

  messageOwner(): void {
    if (!this.offer) {
      return;
    }
    if (!this.isLoggedIn) {
      this.auth.login(this.router.url);
      return;
    }
    this.router.navigate(['/messages'], {queryParams: {with: this.offer.ownerEmail}}).then();
  }

  onDayClicked(date: Date): void {
    const key = this.dayKey(date);
    this.selectedDayLabel = `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}/${date.getFullYear()}`;
    this.selectedDayBookings = this.bookings.filter((b) => this.dayKey(new Date(b.bookedDate)) === key);
    this.dayDialogVisible = true;
  }

  private dayKey(d: Date): string {
    return `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`;
  }

  private getBookings() {
    this.bookingService.getBookedOffers().subscribe(bookings => {
      const all = bookings ?? [];
      this.bookings = this.offer ? all.filter((b) => b.offerId === this.offer?.id) : all;
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
        if (this.isLoggedIn) {
          this.getBookings();
        }
      } else if (!this.offer) {
        this.loadError = 'Offer not found';
      }
    });
  }

  cancelBooking(id: number) {
    if (!this.isLoggedIn) {
      this.auth.login(this.router.url);
      return;
    }
    this.bookingService.deleteBooking(id).subscribe(() => {
      this.dayDialogVisible = false;
      this.getBookings();
    });
  }

  deleteBooking(id: number) {
    this.cancelBooking(id);
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
    const raw = String(bookingForm.value.dateToBook ?? this.bookingDateText ?? '').trim();
    const iso = this.toIso(raw);
    if (!iso) {
      this.bookingError = 'Use format dd/mm/yyyy.';
      return;
    }
    bookingForm.value.dateToBook = iso;
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

  private toIso(raw: string): string | null {
    const m = raw.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
    if (m) {
      return `${m[3]}-${m[2]}-${m[1]}`;
    }
    if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
      return raw;
    }
    return null;
  }
}
