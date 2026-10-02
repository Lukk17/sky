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
            const byId = new Map((all ?? []).map((o) => [o.id, o]));
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
        this.offerService.getUserOffers().subscribe({
          next: (o) => { this.myOffers = o ?? []; },
          error: (e) => this.handleError(e),
        });
      }
    });
  }

  openOffer(id: string): void {
    const found = this.myOffers.find((o) => o.id === id);
    if (found) {
      this.offerService.detailedOffer = found;
    }
    this.router.navigate(['/offerDetails'], {queryParams: {offerId: id}}).then();
  }

  private handleError(error: { message?: string }) {
    this.error = error.message ?? null;
  }
}
