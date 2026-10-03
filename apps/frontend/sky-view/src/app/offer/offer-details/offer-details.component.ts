import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {maskDateInput, toIsoDate} from '../../utils/date-input.util';
import {CalendarEvent} from 'angular-calendar';
import {Offer, OfferService} from '../../services/offer.service';
import {SkyAuthService} from '../../services/sky-auth.service';
import {Location} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {Booking, BookingService} from '../../services/booking.service';

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
  bookingConfirmVisible = false;
  confirmedDateLabel = '';
  selectedDayLabel = '';
  selectedDayBookings: Booking[] = [];
  bookingDateText = '';
  pickerVisible = false;
  pickerDate: Date = new Date();

  galleryPhotos(): { url: string }[] {
    const fromCover = this.coverUrl() ? [{url: this.coverUrl() as string}] : [];
    const rest = (this.offer?.gallery ?? []).map((g) => ({url: g.url})).filter((g) => !!g.url && g.url !== this.coverUrl());
    return [...fromCover, ...rest];
  }

  onDateFocus(): void {
    if (!this.bookingDateText) {
      this.bookingDateText = '';
    }
  }

  onDateInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.bookingDateText = maskDateInput(input.value ?? '');
    input.value = this.bookingDateText;
  }

  onPickerDay(date: Date): void {
    const dd = String(date.getDate()).padStart(2, '0');
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    this.bookingDateText = `${dd}/${mm}/${date.getFullYear()}`;
    this.pickerDate = date;
    this.pickerVisible = false;
  }

  isDateValid(): boolean {
    return this.toIso((this.bookingDateText ?? '').trim()) !== null;
  }
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

  private readonly destroyRef = inject(DestroyRef);

  constructor(private offerService: OfferService, private auth: SkyAuthService, private bookingService: BookingService,
              private location: Location, private router: Router, private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.onKeydown = (e: KeyboardEvent) => this.onGalleryKey(e);
    window.addEventListener('keydown', this.onKeydown);
    this.destroyRef.onDestroy(() => {
      if (this.onKeydown) {
        window.removeEventListener('keydown', this.onKeydown);
      }
    });
    this.auth.currentUser$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next: (email) => {
      this.isLoggedIn = email != null;
      this.isOwner = email != null && this.offer?.ownerEmail === email;
      if (this.isLoggedIn) {
        this.getBookings();
      }
    },
      error: (e: { message?: string }) => {
        this.loadError = e?.message ?? 'Failed to load session.';
      }});
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (params) => {
        const id = String(params['offerId'] ?? '');
        if (id.length > 0 && this.offer?.id !== id) {
          this.loadOfferById(id);
        }
      },
      error: (e: { message?: string }) => {
        this.loadError = e?.message ?? 'Failed to read route.';
      }});
  }

  private onKeydown: ((e: KeyboardEvent) => void) | null = null;

  private onGalleryKey(e: KeyboardEvent): void {
    if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
    const photos = this.galleryPhotos();
    if (photos.length < 2) return;
    const current = this.coverUrl();
    let idx = photos.findIndex((ph) => ph.url === current);
    idx = e.key === 'ArrowRight' ? (idx + 1) % photos.length : (idx - 1 + photos.length) % photos.length;
    this.selectedPhotoUrl = photos[idx].url;
  }

  editOffer(offer: Offer) {
    this.router.navigate(['/editOffer'], {queryParams: {offerId: offer.id}}).then();
  }

  deleteOffer(id: string) {
    this.offerService.deleteOffer(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.router.navigate(['/myOffers']).then(),
      error: () => {
        this.loadError = 'Could not delete the offer. Please try again.';
      }});
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
    this.bookingService.getBookedOffers().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next: (bookings) => {
      const all = bookings ?? [];
      this.bookings = this.offer ? all.filter((b) => b.offerId === this.offer?.id) : all;
      this.events = this.bookings.map((b) => ({
        start: new Date(b.bookedDate),
        title: `Booked by ${b.bookingUser}`,
        color: BOOKED_COLOR,
        allDay: true,
      }));
      },
      error: () => {
        this.loadError = 'Could not load bookings. Please try again.';
      }});
  }

  bookingError: string | null = null;

  private loadOfferById(id: string) {
    this.offerService.getOfferById(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next: (found) => {
      if (found) {
        this.offer = found;
        this.loadError = null;
        if (this.isLoggedIn) {
          this.getBookings();
        }
      } else if (!this.offer) {
        this.loadError = 'Offer not found';
      }
      },
      error: () => {
        this.loadError = 'Could not load the offer. Please try again.';
      }});
  }

  cancelBooking(id: number) {
    if (!this.isLoggedIn) {
      this.auth.login(this.router.url);
      return;
    }
    this.bookingService.deleteBooking(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.dayDialogVisible = false;
        this.getBookings();
      },
      error: () => {
        this.loadError = 'Could not cancel the booking. Please try again.';
      }});
  }

  deleteBooking(id: number) {
    this.cancelBooking(id);
  }

  onSubmit(dateToBookRaw: string, offer: Offer | null) {
    if (!offer) {
      return;
    }
    if (!this.isLoggedIn) {
      this.auth.login(this.router.url);
      return;
    }
    this.bookingError = null;
    const raw = String(dateToBookRaw ?? this.bookingDateText ?? '').trim();
    const iso = this.toIso(raw);
    if (!iso) {
      this.bookingError = 'Use format dd/mm/yyyy.';
      return;
    }
    this.bookingService.addBooking(offer, iso).subscribe({
      next: () => {
        this.confirmedDateLabel = raw;
        this.bookingConfirmVisible = true;
        this.bookingDateText = '';
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
    return toIsoDate(raw);
  }
}
