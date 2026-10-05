import {Component, OnInit, ChangeDetectionStrategy, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NgForm} from '@angular/forms';
import {Offer, OfferDraft, OfferPhoto} from '../../services/offer.service';
import {OfferService} from '../../services/offer.service';
import {ActivatedRoute, Router} from '@angular/router';

export const MAX_PHOTO_BYTES = 5 * 1024 * 1024;

@Component({
    selector: 'app-edit-offer',
    templateUrl: './edit-offer.component.html',
    styleUrls: ['./edit-offer.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class EditOfferComponent implements OnInit {
  error: string | null = null;
  galleryError: string | null = null;
  galleryBusy = false;
  uploading = false;
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

  gallery(): OfferPhoto[] {
    return [...(this.offer?.gallery ?? [])].sort((a, b) => a.position - b.position);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file || !this.offer) return;
    this.galleryError = null;
    if (!file.type.startsWith('image/')) {
      this.galleryError = 'Only image files are allowed.';
      return;
    }
    if (file.size > MAX_PHOTO_BYTES) {
      this.galleryError = 'Photo must be smaller than 5 MB.';
      return;
    }
    this.uploading = true;
    this.offerService.uploadGalleryPhoto(this.offerId, file).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (updated) => {
        this.uploading = false;
        if (updated) this.offer = updated;
      },
      error: () => {
        this.uploading = false;
        this.galleryError = 'Could not upload the photo. Please try again.';
      }});
  }

  removePhoto(photoId: string): void {
    if (!this.offer) return;
    this.galleryBusy = true;
    this.galleryError = null;
    this.offerService.deleteGalleryPhoto(this.offerId, photoId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (updated) => {
        this.galleryBusy = false;
        if (updated) this.offer = updated;
      },
      error: () => {
        this.galleryBusy = false;
        this.galleryError = 'Could not remove the photo. Please try again.';
      }});
  }

  setCover(photoId: string): void {
    if (!this.offer) return;
    this.galleryBusy = true;
    this.galleryError = null;
    this.offerService.setGalleryCover(this.offerId, photoId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (updated) => {
        this.galleryBusy = false;
        if (updated) this.offer = updated;
      },
      error: () => {
        this.galleryBusy = false;
        this.galleryError = 'Could not set the cover photo. Please try again.';
      }});
  }

  onSubmit(offerForm: NgForm) {
    const v = offerForm.value as Record<string, string | number>;
    const draft: OfferDraft = {hotelName: String(v['hotelName'] ?? ''), description: String(v['description'] ?? ''), price: Number(v['price'] ?? 0), roomCapacity: Number(v['roomCapacity'] ?? 0), city: String(v['city'] ?? ''), country: String(v['country'] ?? '')};
    if (!draft.hotelName.trim() || !draft.description.trim() || !draft.city.trim() || !draft.country.trim() || !(draft.price > 0) || !(draft.roomCapacity > 0)) {
      this.error = 'Please fill in all required fields.';
      return;
    }
    this.error = null;
    this.offerService.editOffer(this.offerId, draft).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.router.navigate(['/my-offers']).then(),
      error: () => {
        this.error = 'Could not save the offer. Please try again.';
      }});
  }
}
