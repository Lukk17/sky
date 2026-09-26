import {Component, OnInit, ChangeDetectionStrategy} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {SkyAuthService} from '../services/sky-auth.service';

import {NgForm} from '@angular/forms';
import {OfferService} from '../services/offer.service';

@Component({
    selector: 'app-header',
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class HeaderComponent implements OnInit {
  isAuth = false;
  userEmail: string | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private offerService: OfferService,
    private skyAuth: SkyAuthService,
  ) {
  }

  ngOnInit() {
    this.skyAuth.currentUser$.subscribe((email) => {
      this.userEmail = email;
      this.isAuth = email != null;
    });
  }

  routeToHome() {
    //  instead of using routerLink in html file router can be used in method here
    //  if relative path is needed it can be injected to constructor and used in method
    // now it will add this to previous path
    // this.router.navigate(['/'], {relativeTo: this.route}).then(r => this.logger.log("Route to home"))
    this.router.navigate(['/']).then();
  }

  login() {
    this.skyAuth.login(this.router.url);
  }

  logout() {
    this.isAuth = false;
    this.skyAuth.logout();
  }

  search(searchForm: NgForm) {
    this.offerService.searchOffer(searchForm.value.search).subscribe(offers => {
      this.offerService.searched = offers;
      // mid navigating to "/home" required to reload "/offerSearch" items [workaround]
      this.router.navigate(['/home']).then(() => {
        this.router.navigate(['/offerSearch']).then();
      });
    });
  }
}
