import {Component, ChangeDetectionStrategy} from '@angular/core';
import {NgForm} from '@angular/forms';
import {OfferService} from '../../services/offer.service';
import {Router} from '@angular/router';

@Component({
    selector: 'app-add-offer',
    templateUrl: './add-offer.component.html',
    styleUrls: ['./add-offer.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class AddOfferComponent {
  error: string | null = null;

  constructor(private offerService: OfferService, private router: Router) {
  }

  onSubmit(offerForm: NgForm) {

    this.offerService.addOffer(offerForm).subscribe(() => {
      this.router.navigate(['/myOffers']).then();
    });
  }
}
