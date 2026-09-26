import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {PersonalBooking} from '../../services/booking.service';
import {SkyAuthService} from '../../services/sky-auth.service';

@Component({
    selector: 'app-user-details',
    templateUrl: './user-details.component.html',
    styleUrls: ['./user-details.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class UserDetailsComponent implements OnInit {
  error: string | null = null;
  user: string | null | undefined = null;
  bookedOffers: PersonalBooking[] = [];

  constructor(private skyAuthService: SkyAuthService) {
  }

  ngOnInit() {
    this.skyAuthService.currentUser$.subscribe((email) => {
      this.user = email;
    });
  }

  private handleError(error: { message?: string }) {
    this.error = error.message ?? null;
  }
}
