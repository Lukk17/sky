import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {Router} from '@angular/router';
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

  constructor(private skyAuthService: SkyAuthService, private bookingService: BookingService,
    private offerService: OfferService, private router: Router) {
  }

  ngOnInit() {
    this.skyAuthService.currentUser$.subscribe((email) => {
      this.user = email;
      if (email) {
        this.offerService.getAllOffers().subscribe({
          next: (all) => {
            const list = all ?? [];
            const byId = new Map(list.map((o) => [o.id, o]));
            const wanted = email.trim().toLowerCase();
            this.offerService.getUserOffers().subscribe({
              next: (owned) => {
                this.myOffers = (owned && owned.length > 0)
                  ? owned
                  : list.filter((o) => (o.ownerEmail ?? '').toLowerCase() === wanted);
              },
              error: () => {
                this.myOffers = list.filter((o) => (o.ownerEmail ?? '').toLowerCase() === wanted);
              },
            });
            this.bookingService.getBookedOffers().subscribe({
              next: (bookings) => {
                this.bookedOffers = (bookings ?? []).map(
                  (x) => new PersonalBooking(byId.get(x.offerId)?.hotelName ?? x.offerId, String(x.id), x.offerId, x.bookedDate)
                );
              },
              error: (e) => this.handleError(e),
            });
          },
          error: (e) => this.handleError(e),
        });
      }
    });
  }

  openOffer(id: string): void {
    const found = this.myOffers.find((o) => o.id === id);
    if (found) {
      this.offerService.detailedOffer = found;
    } else {
      this.offerService.getAllOffers().subscribe({
        next: (all) => {
          const match = (all ?? []).find((o) => o.id === id);
          if (match) {
            this.offerService.detailedOffer = match;
          }
        },
      });
    }
    this.router.navigate(['/offerDetails'], {queryParams: {offerId: id}}).then();
  }

  private handleError(error: { message?: string }) {
    this.error = error.message ?? null;
  }
}
