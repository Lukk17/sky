import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Router} from '@angular/router';
import {forkJoin} from 'rxjs';
import {PersonalBooking, BookingService} from '../../services/booking.service';
import {SkyAuthService} from '../../services/sky-auth.service';
import {Offer, OfferService} from '../../services/offer.service';

@Component({
    selector: 'app-user-details',
    templateUrl: './user-details.component.html',
    styleUrls: ['./user-details.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class UserDetailsComponent implements OnInit {
  error: string | null = null;
  user: string | null | undefined = null;
  bookedOffers: PersonalBooking[] = [];
  myOffers: Offer[] = [];

  private readonly destroyRef = inject(DestroyRef);

  constructor(private skyAuthService: SkyAuthService, private bookingService: BookingService,
    private offerService: OfferService, private router: Router) {
  }

  ngOnInit() {
    this.skyAuthService.currentUser$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({next: (email) => {
      this.user = email;
      if (email) {
        forkJoin({
          all: this.offerService.getAllOffers(),
          owned: this.offerService.getUserOffers(),
          bookings: this.bookingService.getBookedOffers(),
        }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: ({all, owned, bookings}) => {
            const list = all ?? [];
            const byId = new Map(list.map((o) => [o.id, o]));
            const wanted = email.trim().toLowerCase();
            this.myOffers = (owned && owned.length > 0)
              ? owned
              : list.filter((o) => (o.ownerEmail ?? '').toLowerCase() === wanted);
            this.bookedOffers = (bookings ?? []).map(
              (x) => new PersonalBooking(byId.get(x.offerId)?.hotelName ?? x.offerId, x.id, x.offerId, x.bookedDate)
            );
          },
          error: (e) => this.handleError(e),
        });
      }
    },
      error: (e) => this.handleError(e)});
  }

  openOffer(id: string): void {
    this.router.navigate(['/offer-details'], {queryParams: {offerId: id}}).then();
  }

  private handleError(error: { message?: string }) {
    this.error = error.message ?? null;
  }
}
