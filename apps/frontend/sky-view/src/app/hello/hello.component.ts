import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Offer, OfferService} from '../services/offer.service';


@Component({
    selector: 'app-hello',
    templateUrl: './hello.component.html',
    styleUrls: ['./hello.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class HelloComponent implements OnInit {

  offers: Offer[] = [];
  error: string | null = null;
  private readonly destroyRef = inject(DestroyRef);

  constructor(private offerService: OfferService) {
  }

  ngOnInit() {
    this.getAllOffers();
  }

  getAllOffers() {
    this.offerService.getAllOffers()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (offers) => {
          this.offers = offers ?? [];
        },
        error: () => {
          this.error = 'Could not load offers. Please try again.';
        }});
  }
}
