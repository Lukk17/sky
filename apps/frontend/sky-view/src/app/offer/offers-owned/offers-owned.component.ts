import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Offer, OfferService} from '../../services/offer.service';

@Component({
    selector: 'app-my-offers',
    templateUrl: './offers-owned.component.html',
    styleUrls: ['./offers-owned.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class OffersOwnedComponent implements OnInit {
  offers: Offer[] = [];
  error: string | null = null;
  private readonly destroyRef = inject(DestroyRef);

  constructor(private offerService: OfferService) {
  }

  ngOnInit(): void {
    this.getUserOffers();
  }

  getUserOffers() {
    this.offerService.getUserOffers().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (offers) => {
          this.offers = offers ?? [];
        },
        error: () => {
          this.error = 'Could not load your offers. Please try again.';
        }}
    );
  }
}
