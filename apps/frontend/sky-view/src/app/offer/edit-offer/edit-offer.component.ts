import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NgForm} from '@angular/forms';
import {Offer, OfferDraft} from '../../services/offer.service';
import {OfferService} from '../../services/offer.service';
import {ActivatedRoute, Router} from '@angular/router';

@Component({
    selector: 'app-edit-offer',
    templateUrl: './edit-offer.component.html',
    styleUrls: ['./edit-offer.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class EditOfferComponent implements OnInit {
  error: string | null = null;
  offer: Offer | null = null;
  private offerId = '';
  private readonly destroyRef = inject(DestroyRef);

  constructor(private offerService: OfferService, private router: Router, private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (params) => {
        this.offerId = String(params['offerId'] ?? '');
        if (this.offerId) {
          this.offerService.getOfferById(this.offerId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: (found) => {
              this.offer = found;
              if (!found) this.error = 'Offer not found.';
            },
            error: () => {
              this.error = 'Could not load the offer. Please try again.';
            }});
        }
      },
      error: () => {
        this.error = 'Could not read the route. Please try again.';
      }});
  }

  onSubmit(offerForm: NgForm) {
    const v = offerForm.value as Record<string, string | number>;
    const draft: OfferDraft = {hotelName: String(v['hotelName'] ?? ''), description: String(v['description'] ?? ''), price: Number(v['price'] ?? 0), roomCapacity: Number(v['roomCapacity'] ?? 0), city: String(v['city'] ?? ''), country: String(v['country'] ?? ''), photoPath: String(v['photoPath'] ?? '')};
    this.error = null;
    this.offerService.editOffer(this.offerId, draft).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.router.navigate(['/myOffers']).then(),
      error: () => {
        this.error = 'Could not save the offer. Please try again.';
      }});
  }
}
