import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {Offer, OfferService} from '../../services/offer.service';
import {Router} from '@angular/router';

@Component({
    selector: 'app-offer-search',
    templateUrl: './offer-search.component.html',
    styleUrls: ['./offer-search.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class OfferSearchComponent implements OnInit {

  offers: Offer[] = [];
  error: string | null = null;
  constructor(private offerService: OfferService, private router: Router) {
  }

  ngOnInit(): void {
    this.offers = this.offerService.searched;
  }
}
