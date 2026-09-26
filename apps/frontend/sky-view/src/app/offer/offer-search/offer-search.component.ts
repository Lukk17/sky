import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {Offer, OfferService} from '../../services/offer.service';
import {Router} from '@angular/router';

@Component({
    selector: 'app-offer-search',
    templateUrl: './offer-search.component.html',
    styleUrls: ['./offer-search.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class OfferSearchComponent implements OnInit {

  offers!: Offer[];

  constructor(private offerService: OfferService, private router: Router) {
  }

  ngOnInit(): void {
    this.offers = this.offerService.searched;
  }
}
