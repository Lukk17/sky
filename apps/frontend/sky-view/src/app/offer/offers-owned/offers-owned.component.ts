import {Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {filter} from 'rxjs/operators';
import {Offer, OfferService} from '../../services/offer.service';
import {SkyAuthService} from '../../services/sky-auth.service';

@Component({
    selector: 'app-my-offers',
    templateUrl: './offers-owned.component.html',
    styleUrls: ['./offers-owned.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class OffersOwnedComponent implements OnInit {
  offers: Offer[] = [];
  error: string | null = null;
  private readonly destroyRef = inject(DestroyRef);

  constructor(private offerService: OfferService, private skyAuth: SkyAuthService, private cdr: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.skyAuth.currentUser$.pipe(filter((email) => email != null), takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.getUserOffers(),
      error: () => {
        this.error = 'Could not load your offers. Please try again.';
        this.cdr.markForCheck();
      }});
  }

  getUserOffers() {
    this.offerService.getUserOffers().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (offers) => {
          this.offers = offers ?? [];
          this.error = null;
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Could not load your offers. Please try again.';
          this.cdr.markForCheck();
        }}
    );
  }
}
