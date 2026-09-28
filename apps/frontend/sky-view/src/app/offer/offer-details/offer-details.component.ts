import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {Offer, OfferService} from '../../services/offer.service';
import {SkyAuthService} from '../../services/sky-auth.service';
import {Location} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {Booking, BookingService} from '../../services/booking.service';
import {NgForm} from '@angular/forms';

@Component({
    selector: 'app-offer-details',
    templateUrl: './offer-details.component.html',
    styleUrls: ['./offer-details.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class OfferDetailsComponent implements OnInit {

  offer!: Offer;
  bookings!: Booking[];
  isOwner = false;
  selectedPhotoUrl: string | null = null;

  coverUrl(): string | null {
    return this.selectedPhotoUrl ?? this.offer?.coverPhotoUrl ?? this.offer?.gallery?.[0]?.url ?? this.offer?.photoUrl ?? null;
  }

  selectPhoto(url: string): void {
    this.selectedPhotoUrl = url;
  }

  constructor(private offerService: OfferService, private auth: SkyAuthService, private bookingService: BookingService,
              private location: Location, private router: Router, private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.offer = this.offerService.detailedOffer;
    this.auth.currentUser$.subscribe((email) => {
      this.isOwner = email != null && this.offer.ownerEmail === email;
    });
    this.getBookings();
  }

  editOffer(offer: Offer) {
    this.offerService.editedOffer = offer;
    this.router.navigate(['/editOffer']).then();
  }

  deleteOffer(id: number) {
    this.offerService.deleteOffer(id).subscribe(value => {
      console.log(value);
      this.router.navigate(['/myOffers']).then();
      return value;
    });
  }

  private getBookings() {
    this.bookingService.getBookedOffers().subscribe(bookings => {
      this.bookings = bookings;
    });
  }

  deleteBooking(id: number) {
    this.bookingService.deleteBooking(id).subscribe(value => {
      console.log(value);
      return value;
    });
  }

  onSubmit(bookingForm: NgForm, offer: Offer) {
    console.log(bookingForm);
    this.bookingService.addBooking(bookingForm, offer).subscribe();
  }
}



