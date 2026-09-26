import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {Offer, OfferService} from '../../services/offer.service';

@Component({
    selector: 'app-my-offers',
    templateUrl: './offers-owned.component.html',
    styleUrls: ['./offers-owned.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class OffersOwnedComponent implements OnInit {
  offers!: Offer[];

  constructor(private offerService: OfferService) {
  }

  ngOnInit(): void {
    this.getUserOffers();
  }

  getUserOffers() {
    this.offerService.getUserOffers().subscribe(offers => {
        this.offers = offers;
      }
    );
  }
}
