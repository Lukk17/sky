import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {NgForm} from '@angular/forms';
import {Offer, OfferService} from '../../services/offer.service';
import {Router} from '@angular/router';

@Component({
    selector: 'app-edit-offer',
    templateUrl: './edit-offer.component.html',
    styleUrls: ['./edit-offer.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class EditOfferComponent implements OnInit {
  error: string | null = null;
  offer!: Offer;

  constructor(private offerService: OfferService, private router: Router) {
  }

  ngOnInit(): void {
    // console.log(this.offer.hotelName)
    this.offer = this.offerService.editedOffer;
  }

  onSubmit(offerForm: NgForm) {
    this.offerService.editOffer(offerForm).subscribe(() =>
      this.router.navigate(['/myOffers']).then()
    );
  }
}
