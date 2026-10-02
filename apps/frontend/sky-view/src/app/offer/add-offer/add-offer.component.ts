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
  selectedFile: File | null = null;
  selectedFileName: string | null = null;
  uploading = false;

  constructor(private offerService: OfferService, private router: Router) {
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile = file;
    this.selectedFileName = file?.name ?? null;
  }

  onSubmit(offerForm: NgForm) {
    this.error = null;
    this.offerService.addOffer(offerForm).subscribe({
      next: (offer) => {
        if (this.selectedFile && offer?.id) {
          this.uploading = true;
          this.offerService.uploadPhoto(offer.id, this.selectedFile).subscribe({
            next: () => this.router.navigate(['/myOffers']).then(),
            error: () => {
              this.uploading = false;
              this.error = 'Offer created but photo upload failed. You can add the photo later.';
            },
          });
        } else {
          this.router.navigate(['/myOffers']).then();
        }
      },
      error: () => {
        this.error = 'Could not create the offer. Please try again.';
      },
    });
  }
}
