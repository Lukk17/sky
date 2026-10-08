import {Component, Input, ChangeDetectionStrategy, ChangeDetectorRef} from '@angular/core';
import {Offer, OfferService} from '../../services/offer.service';
import {Router} from '@angular/router';

@Component({
    selector: 'app-offers',
    templateUrl: './offers.component.html',
    styleUrls: ['./offers.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class OffersComponent {
  @Input() offers: Offer[] = [];
  @Input() showOwnerActions = false;
  error = null;
  pendingDelete: Offer | null = null;
  deleteDialogVisible = false;
  deleteError: string | null = null;

  constructor(private offerService: OfferService, private router: Router, private cdr: ChangeDetectorRef  ) {
  }

  editOffer(offer: Offer) {
    this.router.navigate(['/edit-offer'], {queryParams: {offerId: offer.id}}).then();
  }

  askDelete(offer: Offer) {
    this.pendingDelete = offer;
    this.deleteDialogVisible = true;
    this.deleteError = null;
    this.cdr.markForCheck();
  }

  cancelDelete() {
    this.pendingDelete = null;
    this.deleteDialogVisible = false;
    this.cdr.markForCheck();
  }

  confirmDelete() {
    if (!this.pendingDelete) {
      return;
    }
    const id = this.pendingDelete.id;
    this.offerService.deleteOffer(id).subscribe({
      next: () => {
        this.offers = (this.offers ?? []).filter((o) => o.id !== id);
        this.pendingDelete = null;
        this.deleteDialogVisible = false;
        this.deleteError = null;
        this.cdr.markForCheck();
      },
      error: () => {
        this.deleteError = 'Could not delete the offer. Please try again.';
        this.cdr.markForCheck();
      }});
  }

  goToDetails(offer: Offer) {
    this.router.navigate(['/offer-details'], {queryParams: {offerId: offer.id}}).then();
  }
}
