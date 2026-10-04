import {Component, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NgForm} from '@angular/forms';
import {OfferDraft, OfferService} from '../../services/offer.service';
import {Router} from '@angular/router';

export const MAX_PHOTO_BYTES = 5 * 1024 * 1024;

@Component({
    selector: 'app-add-offer',
    templateUrl: './add-offer.component.html',
    styleUrls: ['./add-offer.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class AddOfferComponent {
  error: string | null = null;
  fileError: string | null = null;
  selectedFile: File | null = null;
  selectedFileName: string | null = null;
  uploading = false;
  private readonly destroyRef = inject(DestroyRef);

  constructor(private offerService: OfferService, private router: Router) {
  }

  isFileValid(): boolean {
    return this.fileError == null && this.selectedFile != null;
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.fileError = null;
    this.selectedFile = null;
    this.selectedFileName = null;
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      this.fileError = 'Only image files are allowed.';
      return;
    }
    if (file.size > MAX_PHOTO_BYTES) {
      this.fileError = 'Photo must be smaller than 5 MB.';
      return;
    }
    this.selectedFile = file;
    this.selectedFileName = file.name;
  }

  onSubmit(offerForm: NgForm) {
    this.error = null;
    if (offerForm.invalid) {
      this.error = 'Please fill in all required fields.';
      return;
    }
    if (!this.isFileValid()) {
      this.fileError = this.fileError ?? 'A photo file is required (image, up to 5 MB).';
      return;
    }
    const v = offerForm.value as Record<string, string | number>;
    const draft: OfferDraft = {hotelName: String(v['hotelName'] ?? ''), description: String(v['description'] ?? ''), price: Number(v['price'] ?? 0), roomCapacity: Number(v['roomCapacity'] ?? 0), city: String(v['city'] ?? ''), country: String(v['country'] ?? '')};
    this.offerService.addOffer(draft).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (offer) => {
        if (this.selectedFile && offer?.id) {
          this.uploading = true;
          this.offerService.uploadPhoto(offer.id, this.selectedFile).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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
