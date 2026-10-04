import {Component, Input, ChangeDetectionStrategy} from '@angular/core';
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
  error = null;

  constructor(private offerService: OfferService, private router: Router  ) {
  }

  editOffer(offer: Offer) {
    this.router.navigate(['/editOffer'], {queryParams: {offerId: offer.id}}).then();
  }

  goToDetails(offer: Offer) {
    this.router.navigate(['/offerDetails'], {queryParams: {offerId: offer.id}}).then();
  }
}
